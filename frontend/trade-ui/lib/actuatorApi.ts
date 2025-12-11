const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8091';

export interface RateLimiterConfig {
  limitForPeriod: string;
  limitRefreshPeriod: string;
  timeoutDuration: string;
}

export interface CircuitBreakerConfig {
  slidingWindowSize: string;
  failureRateThreshold: string;
  waitDurationInOpenState: string;
  minimumNumberOfCalls: string;
}

export async function getRateLimiterConfig(): Promise<RateLimiterConfig | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/actuator/config/ratelimiter`);
    if (!response.ok) return null;
    const data = await response.json();
    return {
      limitForPeriod: data.limitForPeriod || '10',
      limitRefreshPeriod: data.limitRefreshPeriod || '5s',
      timeoutDuration: data.timeoutDuration || '0',
    };
  } catch {
    return null;
  }
}

export async function getCircuitBreakerConfig(): Promise<CircuitBreakerConfig | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/actuator/config/circuitbreaker`);
    if (!response.ok) return null;
    const data = await response.json();
    return {
      slidingWindowSize: data.slidingWindowSize || '1',
      failureRateThreshold: data.failureRateThreshold || '1',
      waitDurationInOpenState: data.waitDurationInOpenState || '5s',
      minimumNumberOfCalls: data.minimumNumberOfCalls || '1',
    };
  } catch {
    return null;
  }
}

