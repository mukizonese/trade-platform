import os
import json
import logging
import asyncio
from kafka import KafkaConsumer
from typing import Callable
from models.trade_event import TradeEvent

logger = logging.getLogger(__name__)

class TradeReportsConsumer:
    def __init__(self, message_handler: Callable):
        self.bootstrap_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        self.topic = os.getenv("KAFKA_TOPIC_TRADE_REPORTS", "trade-reports")
        self.group_id = os.getenv("KAFKA_CONSUMER_GROUP_ID", "trade-ai-service-group")
        self.message_handler = message_handler
        self.consumer = None
        self.running = False
        logger.info(f"Kafka consumer ready - topic: {self.topic}")
    
    async def start(self):
        try:
            self.consumer = KafkaConsumer(
                self.topic,
                bootstrap_servers=self.bootstrap_servers,
                group_id=self.group_id,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                auto_offset_reset='earliest',
                enable_auto_commit=True,
                consumer_timeout_ms=1000
            )
            
            self.running = True
            logger.info("Kafka consumer started")
            asyncio.create_task(self._consume_loop())
            
        except Exception as e:
            logger.warning(f"Kafka unavailable, will retry: {e}")
            self.running = True
            asyncio.create_task(self._retry_connection())
    
    async def _consume_loop(self):
        while self.running:
            try:
                if not self.consumer:
                    await asyncio.sleep(1)
                    continue
                
                # Get messages (non-blocking)
                loop = asyncio.get_event_loop()
                messages = await loop.run_in_executor(
                    None,
                    lambda: self.consumer.poll(timeout_ms=1000)
                )
                
                # Process each message
                for topic_partition, message_list in messages.items():
                    for message in message_list:
                        try:
                            event_data = message.value
                            trade_event = TradeEvent(**event_data)
                            await self.message_handler(trade_event)
                        except Exception as e:
                            logger.error(f"Error processing message: {e}")
                
                await asyncio.sleep(0.1)
                
            except Exception as e:
                logger.error(f"Error in consume loop: {e}")
                await asyncio.sleep(1)
    
    async def _retry_connection(self):
        for attempt in range(10):
            try:
                await asyncio.sleep(2 ** attempt)  # Wait longer each time
                logger.info(f"Retrying Kafka connection (attempt {attempt + 1})...")
                
                self.consumer = KafkaConsumer(
                    self.topic,
                    bootstrap_servers=self.bootstrap_servers,
                    group_id=self.group_id,
                    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                    auto_offset_reset='earliest',
                    enable_auto_commit=True,
                    consumer_timeout_ms=1000
                )
                
                logger.info("Connected to Kafka")
                asyncio.create_task(self._consume_loop())
                break
                
            except Exception as e:
                logger.warning(f"Retry {attempt + 1} failed: {e}")
    
    async def stop(self):
        self.running = False
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka consumer stopped")
