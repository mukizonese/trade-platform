'use client';

import { useState, useMemo, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { submitTrade, getAllTradesFromCache, type TradeDto, type CachedTrade, ServiceUnavailableError } from '@/lib/tradeApi';
import RateLimitTestPanel from './RateLimitTestPanel';
import CircuitBreakerTestPanel from './CircuitBreakerTestPanel';
import toast from 'react-hot-toast';
import { handleTradeError } from '@/lib/handleTradeError';

type SortField = 'tradeId' | 'version' | 'counterPartyId' | 'bookId' | 'maturityDate' | 'expired' | 'createdDate' | 'lastUpdatedAt' | null;
type SortDirection = 'asc' | 'desc';

export default function SimulatorTab() {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<TradeDto>({
    tradeId: '',
    version: 1,
    counterPartyId: '',
    bookId: '',
    maturityDate: '',
  });

  const [filterText, setFilterText] = useState('');
  const [sortField, setSortField] = useState<SortField>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [expandedPanel, setExpandedPanel] = useState<'rateLimit' | 'circuitBreaker' | null>(null);

  // Fetch trades from cache
  const { data: trades = [], isLoading, error: cacheError } = useQuery<CachedTrade[]>({
    queryKey: ['trades', 'cache'],
    queryFn: getAllTradesFromCache,
    refetchInterval: 5000, // Refetch every 5 seconds
    retry: false, // Don't retry on error to avoid spam
  });

  // Handle cache errors
  useEffect(() => {
    if (cacheError instanceof ServiceUnavailableError) {
      toast.error(`Service Unavailable: ${cacheError.message}`, {
        icon: '⚠️',
      });
    }
  }, [cacheError]);

  // Submit trade mutation
  const submitMutation = useMutation({
    mutationFn: (trade: TradeDto) => submitTrade(trade),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['trades', 'cache'] });
        
      if (data.status === 'REJECTED') {
        toast.error(data.message || 'Trade submission rejected', {
          icon: '❌',
          duration: 6000,
        });
      } else {
        toast.success('Trade submitted successfully!');
        setFormData({
          tradeId: '',
          version: 1,
          counterPartyId: '',
          bookId: '',
          maturityDate: '',
        });
      }
    },
    onError: (error) => {
      handleTradeError(error);
    },
  });

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitMutation.mutate(formData);
  };

  const handleChange = (field: keyof TradeDto, value: string | number) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // Helper to get trade field value
  const getTradeValue = (trade: CachedTrade, field: string): string | number | boolean | null | undefined => {
    // Handle camelCase and PascalCase
    const camelCase = trade[field];
    const pascalCase = trade[field.charAt(0).toUpperCase() + field.slice(1)];
    const value = camelCase !== undefined ? camelCase : (pascalCase !== undefined ? pascalCase : '');
    return value;
  };

  // Helper to format date
  const formatDate = (dateValue: string | number | boolean | null | undefined): string => {
    if (!dateValue || dateValue === '-' || dateValue === null || dateValue === undefined) return '-';
    const dateStr = String(dateValue);
    if (!dateStr || dateStr === 'null' || dateStr === 'undefined') return '-';
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return dateStr;
      return date.toLocaleString();
    } catch {
      return dateStr;
    }
  };

  // Filter and sort trades
  const filteredAndSortedTrades = useMemo(() => {
    let result = [...trades];

    // Apply filter
    if (filterText) {
      const filter = filterText.toLowerCase();
      result = result.filter((trade) => {
        const tradeId = String(getTradeValue(trade, 'tradeId')).toLowerCase();
        const counterParty = String(getTradeValue(trade, 'counterPartyId')).toLowerCase();
        const bookId = String(getTradeValue(trade, 'bookId')).toLowerCase();
        const expired = String(getTradeValue(trade, 'expired')).toLowerCase();
        const createdDate = formatDate(getTradeValue(trade, 'createdDate')).toLowerCase();
        const lastUpdatedAt = formatDate(getTradeValue(trade, 'lastUpdatedAt')).toLowerCase();
        return tradeId.includes(filter) || 
               counterParty.includes(filter) || 
               bookId.includes(filter) ||
               expired.includes(filter) ||
               createdDate.includes(filter) ||
               lastUpdatedAt.includes(filter);
      });
    }

    // Apply sorting
    if (sortField) {
      result.sort((a, b) => {
        const aValue = getTradeValue(a, sortField);
        const bValue = getTradeValue(b, sortField);
        
        let comparison = 0;
        
        // Handle boolean values (expired)
        if (typeof aValue === 'boolean' && typeof bValue === 'boolean') {
          comparison = aValue === bValue ? 0 : (aValue ? 1 : -1);
        }
        // Handle numeric values
        else if (typeof aValue === 'number' && typeof bValue === 'number') {
          comparison = aValue - bValue;
        }
        // Handle date strings
        else if (sortField === 'createdDate' || sortField === 'lastUpdatedAt' || sortField === 'maturityDate') {
          const aDate = new Date(String(aValue));
          const bDate = new Date(String(bValue));
          if (!isNaN(aDate.getTime()) && !isNaN(bDate.getTime())) {
            comparison = aDate.getTime() - bDate.getTime();
          } else {
            comparison = String(aValue).localeCompare(String(bValue));
          }
        }
        // Handle string values
        else {
          comparison = String(aValue).localeCompare(String(bValue));
        }
        
        return sortDirection === 'asc' ? comparison : -comparison;
      });
    }

    return result;
  }, [trades, filterText, sortField, sortDirection]);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
      {/* Left Pane - Trade Form */}
      <Card className="lg:col-span-1">
        <CardHeader>
          <CardTitle>Create/Update Trade</CardTitle>
          <CardDescription>Submit a new trade or update an existing one</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="tradeId">Trade ID</Label>
              <Input
                id="tradeId"
                value={formData.tradeId}
                onChange={(e) => handleChange('tradeId', e.target.value)}
                required
                placeholder="e.g., T1"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="version">Version</Label>
              <Input
                id="version"
                type="number"
                min="1"
                value={formData.version}
                onChange={(e) => handleChange('version', parseInt(e.target.value) || 1)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="counterPartyId">Counter Party ID</Label>
              <Input
                id="counterPartyId"
                value={formData.counterPartyId}
                onChange={(e) => handleChange('counterPartyId', e.target.value)}
                required
                placeholder="e.g., CP-1"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="bookId">Book ID</Label>
              <Input
                id="bookId"
                value={formData.bookId}
                onChange={(e) => handleChange('bookId', e.target.value)}
                required
                placeholder="e.g., B1"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="maturityDate">Maturity Date</Label>
              <Input
                id="maturityDate"
                type="date"
                value={formData.maturityDate}
                onChange={(e) => handleChange('maturityDate', e.target.value)}
                required
              />
            </div>

            <Button type="submit" className="w-full" disabled={submitMutation.isPending}>
              {submitMutation.isPending ? 'Submitting...' : 'Submit Trade'}
            </Button>
          </form>

          <RateLimitTestPanel 
            formData={formData} 
            isSubmitting={submitMutation.isPending}
            isExpanded={expandedPanel === 'rateLimit'}
            onToggle={() => setExpandedPanel(expandedPanel === 'rateLimit' ? null : 'rateLimit')}
          />
          <CircuitBreakerTestPanel 
            formData={formData} 
            isSubmitting={submitMutation.isPending}
            isExpanded={expandedPanel === 'circuitBreaker'}
            onToggle={() => setExpandedPanel(expandedPanel === 'circuitBreaker' ? null : 'circuitBreaker')}
          />
        </CardContent>
      </Card>

      {/* Right Pane - Trades List */}
      <Card className="lg:col-span-3">
        <CardHeader>
          <CardTitle>Trades from Cache</CardTitle>
          <CardDescription>All trades cached in Redis (refreshes every 5 seconds)</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">Loading trades...</div>
          ) : trades.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">No trades found in cache</div>
          ) : (
            <div className="space-y-4">
              {/* Filter Input */}
              <Input
                placeholder="Filter by any field..."
                value={filterText}
                onChange={(e) => setFilterText(e.target.value)}
                className="w-full"
              />

              {/* Trades Table */}
              <div className="border rounded-lg overflow-x-auto">
                <Table className="min-w-full">
                  <TableHeader>
                    <TableRow>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('tradeId')}
                      >
                        Trade ID {sortField === 'tradeId' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('version')}
                      >
                        Version {sortField === 'version' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('counterPartyId')}
                      >
                        Counter Party {sortField === 'counterPartyId' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('bookId')}
                      >
                        Book ID {sortField === 'bookId' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('maturityDate')}
                      >
                        Maturity Date {sortField === 'maturityDate' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('expired')}
                      >
                        Expired {sortField === 'expired' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('createdDate')}
                      >
                        Created Date {sortField === 'createdDate' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                      <TableHead 
                        className="cursor-pointer hover:bg-muted/50 whitespace-nowrap"
                        onClick={() => handleSort('lastUpdatedAt')}
                      >
                        Last Updated {sortField === 'lastUpdatedAt' && (sortDirection === 'asc' ? '↑' : '↓')}
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredAndSortedTrades.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                          No trades match the filter
                        </TableCell>
                      </TableRow>
                    ) : (
                      filteredAndSortedTrades.map((trade, index) => (
                        <TableRow key={index}>
                          <TableCell className="font-medium">
                            {String(getTradeValue(trade, 'tradeId')) || '-'}
                          </TableCell>
                          <TableCell>{String(getTradeValue(trade, 'version')) || '-'}</TableCell>
                          <TableCell>
                            {String(getTradeValue(trade, 'counterPartyId')) || '-'}
                          </TableCell>
                          <TableCell>{String(getTradeValue(trade, 'bookId')) || '-'}</TableCell>
                          <TableCell>
                            {String(getTradeValue(trade, 'maturityDate')) || '-'}
                          </TableCell>
                          <TableCell>
                            {(() => {
                              const expiredValue = String(getTradeValue(trade, 'expired')).toUpperCase();
                              if (expiredValue === 'Y' || expiredValue === 'TRUE') return 'Yes';
                              if (expiredValue === 'N' || expiredValue === 'FALSE') return 'No';
                              return '-';
                            })()}
                          </TableCell>
                          <TableCell>
                            {formatDate(getTradeValue(trade, 'createdDate'))}
                          </TableCell>
                          <TableCell>
                            {formatDate(getTradeValue(trade, 'lastUpdatedAt'))}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>

              {/* Results count */}
              {filterText && (
                <div className="text-sm text-muted-foreground">
                  Showing {filteredAndSortedTrades.length} of {trades.length} trades
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
