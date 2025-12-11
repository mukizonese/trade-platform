'use client';

import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { submitTrade, type TradeDto, type TradeSubmissionResponse } from '@/lib/tradeApi';
import { getRateLimiterConfig, type RateLimiterConfig } from '@/lib/actuatorApi';
import toast from 'react-hot-toast';
import { handleTradeError } from '@/lib/handleTradeError';

interface RateLimitTestPanelProps {
  formData: TradeDto;
  isSubmitting: boolean;
  isExpanded: boolean;
  onToggle: () => void;
}

export default function RateLimitTestPanel({ formData, isSubmitting, isExpanded, onToggle }: RateLimitTestPanelProps) {
  const queryClient = useQueryClient();
  const [testRequestCount, setTestRequestCount] = useState<number>(5);
  const [isTesting, setIsTesting] = useState(false);

  const { data: config } = useQuery<RateLimiterConfig | null>({
    queryKey: ['rateLimiterConfig'],
    queryFn: getRateLimiterConfig,
    refetchInterval: 30000,
    retry: false,
  });

  const handleTest = async () => {
    if (!formData.tradeId || !formData.counterPartyId || !formData.bookId || !formData.maturityDate) {
      toast.error('Please fill in all trade fields before testing rate limits');
      return;
    }

    if (testRequestCount < 1 || testRequestCount > 50) {
      toast.error('Number of requests must be between 1 and 50');
      return;
    }

    setIsTesting(true);
    let successCount = 0;
    let rejectedCount = 0;
    let rateLimitCount = 0;
    let serviceErrorCount = 0;

    const requests = Array.from({ length: testRequestCount }, (_, index) => {
      const testTrade: TradeDto = {
        ...formData,
        tradeId: `${formData.tradeId}-RL-${index + 1}`,
      };
      return submitTrade(testTrade)
        .then((data: TradeSubmissionResponse) => {
          if (data.status === 'REJECTED') {
            rejectedCount++;
            toast.error(data.message || 'Trade rejected', {
              icon: '❌',
              duration: 4000,
            });
            return { rejected: true };
          }
          successCount++;
          return { success: true };
        })
        .catch((error) => {
          const kind = handleTradeError(error);
          if (kind === 'rateLimit') {
            rateLimitCount++;
          } else {
            serviceErrorCount++;
          }
        });
    });

    await Promise.all(requests);
    setIsTesting(false);

    setTimeout(() => {
      if (rateLimitCount > 0 || serviceErrorCount > 0 || rejectedCount > 0) {
        toast.success(
          `Test Complete: ${successCount} accepted, ${rejectedCount} rejected, ${rateLimitCount} rate limited, ${serviceErrorCount} errors`,
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
        <span>Rate Limit Test</span>
        <span className="text-sm">{isExpanded ? '▼' : '▶'}</span>
      </Button>
      {isExpanded && (
        <div className="space-y-4 mt-2">
          <div className="bg-muted/50 rounded-md p-3 space-y-1.5 text-xs">
            <div className="space-y-1">
              <div>
                <span className="text-muted-foreground">limitForPeriod:</span>
                <span className="ml-2 font-mono">{config?.limitForPeriod || '10'}/period</span>
              </div>
              <div>
                <span className="text-muted-foreground">limitRefreshPeriod:</span>
                <span className="ml-2 font-mono">{config?.limitRefreshPeriod || '5s'}</span>
              </div>
              <div>
                <span className="text-muted-foreground">timeoutDuration:</span>
                <span className="ml-2 font-mono">{config?.timeoutDuration || '0'}</span>
              </div>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="testRequestCount">Number of Requests</Label>
            <Input
              id="testRequestCount"
              type="number"
              min="1"
              max="50"
              value={testRequestCount}
              onChange={(e) => setTestRequestCount(parseInt(e.target.value) || 1)}
              placeholder="Enter number of parallel requests"
            />
            <p className="text-xs text-muted-foreground">
              Will send {testRequestCount} parallel requests using the trade data above
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            className="w-full"
            onClick={handleTest}
            disabled={isTesting || isSubmitting}
          >
            {isTesting ? `Sending ${testRequestCount} requests...` : `Test Rate Limits (${testRequestCount} requests)`}
          </Button>
        </div>
      )}
    </div>
  );
}

