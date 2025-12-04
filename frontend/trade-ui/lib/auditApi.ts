// Gateway route
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

// Direct route
const API_BASE_URL_DIRECT = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8088';

export interface AuditDto {
  auditId: string;
  tradeId: string;
  version: number;
  timestamp: string;
  bookId?: string | null;
  counterPartyId?: string | null;
  maturityDate?: string | null;
  expired?: string | null;
  createdDate?: string | null;
  lastUpdatedAt?: string | null;
  source?: string | null;
  validationStatus?: string | null;
  eventType?: string | null;
}

export interface AuditPageResponse {
  content: AuditDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export async function getAuditLogs(
  page: number = 0,
  size: number = 50
): Promise<AuditPageResponse> {
  const response = await fetch(`${API_BASE_URL}/api/audit/trades?page=${page}&size=${size}`);

  if (!response.ok) {
    throw new Error(`Failed to fetch audit logs: ${response.statusText}`);
  }

  return response.json();
}

