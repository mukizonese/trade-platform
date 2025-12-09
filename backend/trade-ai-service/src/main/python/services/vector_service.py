import os
import logging
from typing import List, Dict, Any
import chromadb
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

class VectorService:
    def __init__(self):
        self.host = os.getenv("VECTOR_DB_HOST", "localhost")
        self.port = int(os.getenv("VECTOR_DB_PORT", "8000"))
        self.collection_name = os.getenv("VECTOR_DB_COLLECTION", "trade_reports")
        self.embedding_model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
        self.client = None
        self.collection = None
        logger.info(f"VectorService ready - {self.host}:{self.port}")
    
    async def initialize(self):
        self.client = chromadb.HttpClient(host=self.host, port=self.port)
        self.collection = self.client.get_or_create_collection(name=self.collection_name)
        logger.info(f"Connected to ChromaDB - collection: {self.collection_name}")
    
    def embed_text(self, text: str) -> List[float]:
        """Convert text to a list of numbers (embedding)"""
        embedding = self.embedding_model.encode(text, convert_to_numpy=True)
        return embedding.tolist()
    
    async def add_document(self, document: Dict[str, Any]) -> bool:
        try:
            doc_id = document["id"]
            content = document["content"]
            metadata = document["metadata"]
            
            # Convert text to numbers
            embedding = self.embed_text(content)
            
            self.collection.add(
                embeddings=[embedding],
                documents=[content],
                metadatas=[metadata],
                ids=[doc_id]
            )
            return True
        except Exception as e:
            logger.error(f"Error adding document: {e}")
            return False
    
    async def search_similar(self, query: str, limit: int = 5, min_similarity: float = 0.2) -> List[Dict[str, Any]]:
        try:
            # Convert query to numbers
            query_embedding = self.embed_text(query)
            results = self.collection.query(
                query_embeddings=[query_embedding],
                n_results=limit
            )
            
            # Process results
            documents = []
            if results and "ids" in results and len(results["ids"]) > 0:
                for i in range(len(results["ids"][0])):
                    distance = results["distances"][0][i]
                    similarity = 1.0 - (distance / 2.0)  # ChromaDB uses 0-2 range
                    similarity = max(0.0, min(1.0, similarity))
                    
                    # Only include if similar enough
                    if similarity >= min_similarity:
                        documents.append({
                            "id": results["ids"][0][i],
                            "content": results["documents"][0][i],
                            "metadata": results["metadatas"][0][i],
                            "score": round(similarity, 3)
                        })
            
            logger.info(f"Found {len(documents)} similar documents")
            return documents
        except Exception as e:
            logger.error(f"Error searching: {e}")
            return []
    
    async def get_collection_stats(self) -> Dict[str, Any]:
        try:
            count = self.collection.count()
            return {
                "backend": "chromadb",
                "total_vectors": count,
                "dimension": 384,
                "collection_name": self.collection_name
            }
        except Exception as e:
            logger.error(f"Error getting stats: {e}")
            return {"backend": "error", "total_vectors": 0}
