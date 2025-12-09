/**
 * AI Service API Client
 * Simple API calls to the trade-ai-service
 */

const AI_API_URL = process.env.NEXT_PUBLIC_AI_API_URL || 'http://localhost:8094';

export interface ChatRequest {
  message: string;
  user_id?: string;
  session_id?: string;
  limit?: number;
  min_similarity?: number;
}

export interface ChatSource {
  tradeId?: string;
  version?: number;
  eventType?: string;
  score?: number;
  content?: string;
}

export interface ChatResponse {
  response: string;
  sources: ChatSource[];
  session_id: string;
}

/**
 * Send a chat message to the AI service
 */
export async function sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
  const response = await fetch(`${AI_API_URL}/api/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(`AI service error: ${response.statusText}`);
  }

  return response.json();
}

/**
 * Get AI service statistics
 */
export async function getAIStats() {
  const response = await fetch(`${AI_API_URL}/api/ai/stats`);

  if (!response.ok) {
    throw new Error(`AI service error: ${response.statusText}`);
  }

  return response.json();
}

