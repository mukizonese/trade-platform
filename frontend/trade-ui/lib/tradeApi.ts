// Gateway route
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

// Direct route
const API_BASE_URL_DIRECT = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8088';

export interface TradeDto {
  tradeId: string;
  version: number;
  counterPartyId: string;
  bookId: string;
  maturityDate: string; // YYYY-MM-DD format
}

export interface TradeSubmissionResponse {
  status: string;
  eventType: string;
  reason?: string;
  message?: string;
  trade?: TradeDto;
}

export interface CachedTrade {
  [key: string]: string | number | boolean | null | undefined;
}

// Custom error class for rate limiting
export class RateLimitError extends Error {
  constructor(message: string, public retryAfter?: number) {
    super(message);
    this.name = 'RateLimitError';
  }
}

// Custom error class for service unavailable (circuit breaker)
export class ServiceUnavailableError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ServiceUnavailableError';
  }
}

export async function submitTrade(
  trade: TradeDto,
  source: string = 'UI_SIMULATOR'
): Promise<TradeSubmissionResponse> {
  let response: Response;
  
  try {
    response = await fetch(`${API_BASE_URL}/api/trades?source=${source}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(trade),
    });
  } catch (error) {
    // Network error - service is likely down
    throw new ServiceUnavailableError(
      'Service is unavailable. The trade service may be down or unreachable.'
    );
  }

  // Handle rate limiting (429)
  if (response.status === 429) {
    let errorMessage = 'Rate limit exceeded. Please wait before trying again.';
    const retryAfter = response.headers.get('Retry-After');
    
    try {
      const data = await response.json();
      errorMessage = data.message || data.error || errorMessage;
    } catch {
      // If response is not JSON, use default message
    }
    
    throw new RateLimitError(errorMessage, retryAfter ? parseInt(retryAfter) : undefined);
  }

  // Handle service unavailable (503) - circuit breaker open
  if (response.status === 503) {
    throw new ServiceUnavailableError(
      'Service temporarily unavailable. The circuit breaker is open. Please try again later.'
    );
  }

  // Handle other server errors (500, 502, 504) - service issues
  if (response.status >= 500) {
    throw new ServiceUnavailableError(
      'Service error occurred. The trade service may be experiencing issues.'
    );
  }

  let data: TradeSubmissionResponse;
  try {
    data = await response.json();
  } catch {
    throw new Error('Invalid response from server');
  }

  if (!response.ok) {
    throw new Error(`Failed to submit trade: ${response.statusText}`);
  }

  return data;
}

export async function getAllTradesFromCache(): Promise<CachedTrade[]> {
  let response: Response;
  
  try {
    response = await fetch(`${API_BASE_URL}/api/trades/cache`);
  } catch (error) {
    // Network error - service is likely down
    throw new ServiceUnavailableError(
      'Service is unavailable. Unable to fetch trades from cache.'
    );
  }

  // Handle service unavailable (503) - circuit breaker open
  if (response.status === 503) {
    throw new ServiceUnavailableError(
      'Service temporarily unavailable. The circuit breaker is open.'
    );
  }

  // Handle other server errors (500, 502, 504)
  if (response.status >= 500) {
    throw new ServiceUnavailableError(
      'Service error occurred. Unable to fetch trades.'
    );
  }

  if (!response.ok) {
    throw new Error(`Failed to fetch trades from cache: ${response.statusText}`);
  }

  return response.json();
}

export async function getTradesFromCacheByTradeId(
  tradeId: string
): Promise<CachedTrade[]> {
  const response = await fetch(`${API_BASE_URL}/api/trades/cache/${tradeId}`);

  if (!response.ok) {
    if (response.status === 404) {
      return [];
    }
    throw new Error(`Failed to fetch trades from cache: ${response.statusText}`);
  }

  return response.json();
}

