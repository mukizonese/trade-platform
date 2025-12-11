// handleTradeError.ts
import toast from 'react-hot-toast';
import {
  RateLimitError,
  CircuitBreakerError,
  ServiceUnavailableError,
  NetworkError,
  TradeError,
} from '@/lib/tradeApi';

export function handleTradeError(error: unknown) {
  if (error instanceof RateLimitError) {
    const retryMsg = error.retryAfterSeconds
      ? ` Please wait ${error.retryAfterSeconds} seconds before trying again.`
      : '';
    toast.error(`Rate Limit Exceeded: ${error.message}${retryMsg}`, {
      icon: '🚫',
      duration: 6000,
    });
    return 'rateLimit';
  }

  if (error instanceof CircuitBreakerError) {
    toast.error('Circuit Breaker OPEN: Service is temporarily unavailable', {
      icon: '🔴',
      duration: 6000,
    });
    return 'circuitBreaker';
  }

  if (error instanceof ServiceUnavailableError) {
    toast.error(`Service Unavailable: ${error.message}`, {
      icon: '⚠️',
      duration: 6000,
    });
    return 'serviceUnavailable';
  }

  // Handle business errors (TradeError with 400 status) - check BEFORE NetworkError
  if (error instanceof TradeError && error.status === 400) {
    toast.error(error.message, {
      icon: '❌',
      duration: 6000,
    });
    return 'businessError';
  }

  if (error instanceof NetworkError) {
    toast.error(`Network Error: ${error.message}`, {
      icon: '📡',
      duration: 6000,
    });
    return 'network';
  }

  const message = error instanceof Error ? error.message : 'Unknown error';
  toast.error(`Request Failed: ${message}`, {
    icon: '❌',
  });
  return 'other';
}
