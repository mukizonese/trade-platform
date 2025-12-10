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
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 120000);

  try {
    const response = await fetch(`${AI_API_URL}/api/ai/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`AI service error: ${response.statusText}`);
    }

    return response.json();
  } catch (error) {
    clearTimeout(timeoutId);
    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        throw new Error('Request timeout - AI service took too long to respond');
      }
      throw error;
    }
    throw new Error('Failed to fetch: Network error');
  }
}

/**
 * Get AI service statistics
 */
export async function getAIStats() {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 30000);

  try {
    const response = await fetch(`${AI_API_URL}/api/ai/stats`, {
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`AI service error: ${response.statusText}`);
    }

    return response.json();
  } catch (error) {
    clearTimeout(timeoutId);
    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        throw new Error('Request timeout');
      }
      throw error;
    }
    throw new Error('Failed to fetch: Network error');
  }
}

