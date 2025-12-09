from typing import Dict, Any, Optional
from pydantic import BaseModel

class TradeEvent(BaseModel):
    eventId: str
    eventType: str
    tradeId: str
    version: int
    timestamp: str
    payload: Dict[str, Any]
    reason: Optional[str] = None
    
    def to_document(self) -> Dict[str, Any]:
        parts = [
            f"Trade ID: {self.tradeId}",
            f"Version: {self.version}",
            f"Event Type: {self.eventType}",
        ]
        
        # Add trade details from payload
        if self.payload:
            fields = {
                "counterPartyId": "Counter Party",
                "bookId": "Book",
                "maturityDate": "Maturity Date",
                "createdDate": "Created Date",
                "lastUpdatedAt": "Last Updated"
            }
            
            for key, label in fields.items():
                if key in self.payload:
                    parts.append(f"{label}: {self.payload[key]}")
            
            if "expired" in self.payload:
                expired_value = "Yes" if self.payload["expired"] == "Y" else "No"
                parts.append(f"Expired: {expired_value}")
        
        if self.reason:
            parts.append(f"Reason: {self.reason}")
        
        content = ". ".join(parts)
        
        metadata = {
            "tradeId": self.tradeId,
            "version": self.version,
            "eventType": self.eventType,
            "timestamp": self.timestamp,
            "eventId": self.eventId
        }
        
        if self.payload:
            for key in ["createdDate", "lastUpdatedAt", "maturityDate", 
                       "counterPartyId", "bookId"]:
                if key in self.payload:
                    metadata[key] = self.payload[key]
            
            if "expired" in self.payload and self.payload["expired"] == "Y":
                metadata["expired"] = "Expired"
        
        return {
            "id": f"{self.tradeId}_v{self.version}_{self.eventId}",
            "title": f"Trade {self.tradeId} - {self.eventType}",
            "content": content,
            "source": "trade_reports",
            "metadata": metadata
        }
