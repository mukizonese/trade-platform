const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8088';

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
  [key: string]: any;
}

export async function submitTrade(
  trade: TradeDto,
  source: string = 'UI_SIMULATOR'
): Promise<TradeSubmissionResponse> {
  const response = await fetch(`${API_BASE_URL}/api/trades?source=${source}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(trade),
  });

  const data = await response.json();

  // If status is REJECTED, throw error with the validation message
  if (data.status === 'REJECTED') {
    throw new Error(data.message || `Validation failed: ${data.reason || 'Unknown error'}`);
  }

  if (!response.ok) {
    throw new Error(`Failed to submit trade: ${response.statusText}`);
  }

  return data;
}

export async function getAllTradesFromCache(): Promise<CachedTrade[]> {
  const response = await fetch(`${API_BASE_URL}/api/trades/cache`);

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

