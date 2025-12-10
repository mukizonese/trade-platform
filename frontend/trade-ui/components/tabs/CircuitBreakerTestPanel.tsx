'use client';

import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { submitTrade, type TradeDto, type TradeSubmissionResponse, ServiceUnavailableError, RateLimitError } from '@/lib/tradeApi';
import { getCircuitBreakerConfig, type CircuitBreakerConfig } from '@/lib/actuatorApi';
import toast from 'react-hot-toast';
import { handleTradeError } from '@/lib/handleTradeError';

interface CircuitBreakerTestPanelProps {
  formData: TradeDto;
  isSubmitting: boolean;
  isExpanded: boolean;
  onToggle: () => void;
}

export default function CircuitBreakerTestPanel({ formData, isSubmitting, isExpanded, onToggle }: CircuitBreakerTestPanelProps) {
  const queryClient = useQueryClient();
  const [requestCount, setRequestCount] = useState<number>(5);
  const [delayMs, setDelayMs] = useState<number>(1000);
  const [isTesting, setIsTesting] = useState(false);

  const { data: config } = useQuery<CircuitBreakerConfig | null>({
    queryKey: ['circuitBreakerConfig'],
    queryFn: getCircuitBreakerConfig,
    refetchInterval: 30000,
    retry: false,
  });

  const handleTest = async () => {
    if (!formData.tradeId || !formData.counterPartyId || !formData.bookId || !formData.maturityDate) {
      toast.error('Please fill in all trade fields before testing circuit breaker');
      return;
    }

    if (requestCount < 1 || requestCount > 50) {
      toast.error('Number of requests must be between 1 and 50');
      return;
    }

    if (delayMs < 0 || delayMs > 10000) {
      toast.error('Delay must be between 0 and 10000 ms');
      return;
    }

    setIsTesting(true);
    let successCount = 0;
    let rejectedCount = 0;
    let serviceErrorCount = 0;
    let previousWasError = false;

    for (let i = 0; i < requestCount; i++) {
      const testTrade: TradeDto = {
        ...formData,
        tradeId: `${formData.tradeId}-CB-${i + 1}`,
      };

      try {
        const data = await submitTrade(testTrade);
        successCount++;
        if (previousWasError) {
          toast.success('Circuit Breaker CLOSED: Service is back online', {
            icon: '🟢',
            duration: 4000,
          });
          previousWasError = false;
        }
      } catch (error) {
        previousWasError = true;
        const kind = handleTradeError(error);
        if (kind === 'rateLimit') {
          // if you want, you can track RL here too
          serviceErrorCount++;
        } else {
          serviceErrorCount++;
        }
      }

      if (i < requestCount - 1 && delayMs > 0) {
        await new Promise(resolve => setTimeout(resolve, delayMs));
      }
    }

    setIsTesting(false);

    setTimeout(() => {
      if (serviceErrorCount > 0 || rejectedCount > 0) {
        toast.success(
          `Test Complete: ${successCount} accepted, ${rejectedCount} rejected, ${serviceErrorCount} errors`,
          { duration: 4000 }
        );
      } else {
        toast.success(`All ${successCount} requests succeeded!`, { duration: 3000 });
      }
    }, 500);

    queryClient.invalidateQueries({ queryKey: ['trades', 'cache'] });
  };

  return (
    <div className="mt-3 pt-2 border-t">
      <Button
        type="button"
        variant="ghost"
        className="w-full justify-between h-auto py-2 font-semibold text-lg"
        onClick={onToggle}
      >
        <span>Circuit Breaker Test</span>
        <span className="text-sm">{isExpanded ? '▼' : '▶'}</span>
      </Button>
      {isExpanded && (
        <div className="space-y-4 mt-2">
          <div className="bg-muted/50 rounded-md p-3 space-y-1.5 text-xs">
            <div className="space-y-1">
              <div>
                <span className="text-muted-foreground">slidingWindowSize:</span>
                <span className="ml-2 font-mono">{config?.slidingWindowSize || '5'}</span>
              </div>
              <div>
                <span className="text-muted-foreground">failureRateThreshold:</span>
                <span className="ml-2 font-mono">{config?.failureRateThreshold || '50'}%</span>
              </div>
              <div>
                <span className="text-muted-foreground">waitDurationInOpenState:</span>
                <span className="ml-2 font-mono">{config?.waitDurationInOpenState || '10s'}</span>
              </div>
              <div>
                <span className="text-muted-foreground">minimumNumberOfCalls:</span>
                <span className="ml-2 font-mono">{config?.minimumNumberOfCalls || '10'}</span>
              </div>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="cbRequestCount">Number of Requests</Label>
            <Input
              id="cbRequestCount"
              type="number"
              min="1"
              max="50"
              value={requestCount}
              onChange={(e) => setRequestCount(parseInt(e.target.value) || 1)}
              placeholder="Enter number of sequential requests"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="cbDelayMs">Delay Between Requests (ms)</Label>
            <Input
              id="cbDelayMs"
              type="number"
              min="0"
              max="10000"
              value={delayMs}
              onChange={(e) => setDelayMs(parseInt(e.target.value) || 0)}
              placeholder="Delay in milliseconds"
            />
            <p className="text-xs text-muted-foreground">
              Will send {requestCount} sequential requests with {delayMs}ms delay between each
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            className="w-full"
            onClick={handleTest}
            disabled={isTesting || isSubmitting}
          >
            {isTesting ? `Sending ${requestCount} requests...` : `Test Circuit Breaker (${requestCount} requests)`}
          </Button>
        </div>
      )}
    </div>
  );
}

