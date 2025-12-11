// Gateway route
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

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

export class TradeError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly code?: string,
  ) {
    super(message);
    this.name = 'TradeError';
  }
}

export class RateLimitError extends TradeError {
  constructor(message: string, public readonly retryAfterSeconds?: number) {
    super(message, 429, 'RATE_LIMIT_EXCEEDED');
    this.name = 'RateLimitError';
  }
}

export class CircuitBreakerError extends TradeError {
  constructor(message: string) {
    super(message, 503, 'CIRCUIT_BREAKER_OPEN');
    this.name = 'CircuitBreakerError';
  }
}

export class ServiceUnavailableError extends TradeError {
  constructor(message: string) {
    super(message, 503, 'SERVICE_UNAVAILABLE');
    this.name = 'ServiceUnavailableError';
  }
}

export class NetworkError extends TradeError {
  constructor(message: string) {
    super(message);
    this.name = 'NetworkError';
  }
}

export async function submitTrade(
  trade: TradeDto,
  source: string = 'UI_SIMULATOR'
): Promise<TradeSubmissionResponse> {
  const response = await fetch(`${API_BASE_URL}/api/trades?source=${source}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(trade),
  });

  const data = (await response.json().catch(() => ({}))) as TradeSubmissionResponse;
  const reason = data.reason || '';

  if (response.ok) {
    return data;
  }

  // Rate limit
  if (response.status === 429 || reason === 'RATE_LIMIT_EXCEEDED') {
    const retryAfterHeader = response.headers.get('Retry-After');
    const retryAfterSeconds = retryAfterHeader ? Number(retryAfterHeader) : undefined;
    throw new RateLimitError(
      data.message || 'Rate limit exceeded',
      retryAfterSeconds,
    );
  }

  // Circuit breaker open
  if (response.status === 503 && reason === 'CIRCUIT_BREAKER_OPEN') {
    throw new CircuitBreakerError(
      data.message || 'Circuit breaker open: trade service unavailable',
    );
  }

  // Generic service unavailable
  if (response.status === 503) {
    throw new ServiceUnavailableError(
      data.message || 'Trade service unavailable',
    );
  }

  // Business rejections (400 Bad Request) - check if it's a validation/business error
  if (response.status === 400 && data.message) {
    // Check if message contains business validation keywords or if status is REJECTED
    const isBusinessError = data.status === 'REJECTED' || 
                           data.reason === 'PAST_MATURITY' || 
                           data.reason === 'LOWER_VERSION' ||
                           data.reason === 'CLIENT_ERROR' ||
                           data.message.includes('Validation failed') ||
                           data.message.includes('cannot be accepted') ||
                           data.message.includes('rejected');
    
    if (isBusinessError) {
      // Preserve business error message by throwing a TradeError with the message
      throw new TradeError(data.message, 400, data.reason);
    }
  }

  // Network / generic
  throw new NetworkError(
    data.message || `Request failed with status ${response.status}`,
  );
}

export async function getAllTradesFromCache(): Promise<CachedTrade[]> {
  let response: Response;
  
  try {
    response = await fetch(`${API_BASE_URL}/api/trades/cache`);
  } catch {
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

