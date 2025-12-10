import os
import logging
from pathlib import Path
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from dotenv import load_dotenv
from contextlib import asynccontextmanager

from services.vector_service import VectorService
from services.kafka_consumer import TradeReportsConsumer
from services.llm_service import LLMService
from models.trade_event import TradeEvent

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def load_environment():
    current_file = Path(__file__).resolve()
    project_root = current_file.parent.parent.parent.parent.parent.parent
    profile = os.getenv("SPRING_PROFILES_ACTIVE", "local")
    env_file = project_root / f".env.{profile}"
    
    if env_file.exists():
        load_dotenv(env_file, override=True)
        logger.info(f"Loaded: {env_file}")
    else:
        fallback = project_root / ".env"
        if fallback.exists():
            load_dotenv(fallback, override=True)
            logger.info(f"Loaded: {fallback}")

load_environment()

# Global services (initialized on startup)
vector_service = None
kafka_consumer = None
llm_service = None

async def process_trade_event(trade_event: TradeEvent):
    try:
        document = trade_event.to_document()
        await vector_service.add_document(document)
        logger.info(f"Added trade: {trade_event.tradeId} v{trade_event.version}")
    except Exception as e:
        logger.error(f"Error processing trade event: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    global vector_service, kafka_consumer, llm_service
    
    logger.info("Starting Trade AI Service...")
    
    # Initialize vector database
    vector_service = VectorService()
    await vector_service.initialize()
    
    # Initialize LLM service
    llm_service = LLMService()
    
    # Initialize Kafka consumer 
    try:
        kafka_consumer = TradeReportsConsumer(message_handler=process_trade_event)
        await kafka_consumer.start()
        logger.info("Kafka consumer started")
    except Exception as e:
        logger.warning(f"Kafka not available: {e}")
        kafka_consumer = None
    
    logger.info("Service ready!")
    yield
    
    if kafka_consumer:
        await kafka_consumer.stop()


# Create FastAPI app
app = FastAPI(title="Trade AI Service", version="1.0.0", lifespan=lifespan)

class ChatRequest(BaseModel):
    message: str
    user_id: Optional[str] = "anonymous"
    session_id: Optional[str] = "default"
    limit: Optional[int] = 5
    min_similarity: Optional[float] = 0.2

class ChatResponse(BaseModel):
    response: str
    sources: List[dict]
    session_id: str

@app.get("/health")
async def health():
    stats = await vector_service.get_collection_stats() if vector_service else {}
    return {
        "status": "healthy",
        "vector_db": stats,
        "kafka": kafka_consumer is not None and kafka_consumer.running
    }

@app.post("/api/ai/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    if not vector_service or not llm_service:
        raise HTTPException(status_code=503, detail="Service not ready")
    
    # Search for similar trades
    min_sim = request.min_similarity if request.min_similarity is not None else 0.2
    similar_trades = await vector_service.search_similar(
        query=request.message,
        limit=request.limit or 5,
        min_similarity=min_sim
    )
    
    if similar_trades:
        response_text = await llm_service.generate_response(request.message, similar_trades)
    else:
        response_text = f"No trades found matching: '{request.message}'"
    
    # Format sources
    sources = [{
        "tradeId": doc["metadata"].get("tradeId"),
        "version": doc["metadata"].get("version"),
        "eventType": doc["metadata"].get("eventType"),
        "score": doc["score"],
        "content": doc["content"][:300]
    } for doc in similar_trades]
    
    return ChatResponse(
        response=response_text,
        sources=sources,
        session_id=request.session_id or "default"
    )

@app.get("/api/ai/stats")
async def stats():
    if not vector_service:
        raise HTTPException(status_code=503, detail="Service not ready")
    return await vector_service.get_collection_stats()

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("TRADE_AI_SERVICE_PORT", "8094"))
    uvicorn.run(app, host="0.0.0.0", port=port)
