import os
import logging
from typing import Dict, Any, List
import aiohttp

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        self.use_ollama = os.getenv("USE_OLLAMA", "true").lower() == "true"
        self.ollama_host = os.getenv("OLLAMA_HOST", "localhost:11434")
        self.ollama_model = os.getenv("OLLAMA_MODEL", "gemma:2b")
        logger.info(f"LLMService ready - Ollama: {self.use_ollama}")
    
    async def generate_response(self, query: str, context: List[Dict[str, Any]]) -> str:
        context_text = self._build_context(context)
        prompt = f"""You are a helpful AI assistant for a trade platform. 
Answer the user's question based on the following trade information.

Trade Information:
{context_text}

User Question: {query}

Please provide a clear answer based on the trade information above."""

        try:
            if self.use_ollama:
                return await self._call_ollama(prompt)
            else:
                return self._simple_response(query, context)
        except Exception as e:
            logger.error(f"Error generating response: {e}", exc_info=True)
            return self._simple_response(query, context)
    
    def _build_context(self, context: List[Dict[str, Any]]) -> str:
        """Convert trade documents to readable text"""
        if not context:
            return "No trade information found."
        
        parts = []
        for i, doc in enumerate(context, 1):
            content = doc.get("content", "")
            metadata = doc.get("metadata", {})
            trade_id = metadata.get("tradeId", "Unknown")
            parts.append(f"{i}. Trade {trade_id}: {content}")
        
        return "\n\n".join(parts)
    
    async def _call_ollama(self, prompt: str) -> str:
        url = f"http://{self.ollama_host}/api/generate"
        payload = {
            "model": self.ollama_model,
            "prompt": prompt,
            "stream": False,
            "options": {"num_predict": 500, "temperature": 0.7}
        }
        
        logger.info(f"Calling Ollama: {url}")
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(url, json=payload, timeout=aiohttp.ClientTimeout(total=90)) as response:
                    if response.status == 200:
                        result = await response.json()
                        return result.get("response", "").strip()
                    else:
                        error_text = await response.text()
                        raise Exception(f"Ollama HTTP {response.status}: {error_text}")
        except Exception as e:
            logger.error(f"Ollama call failed: {e}")
            raise
    
    def _simple_response(self, query: str, context: List[Dict[str, Any]]) -> str:
        """Simple response when AI is unavailable"""
        if not context:
            return f"I couldn't find any trades matching: '{query}'"
        
        trade_ids = [doc.get("metadata", {}).get("tradeId", "Unknown") for doc in context]
        return f"Found {len(context)} trade(s): {', '.join(trade_ids[:5])}"
