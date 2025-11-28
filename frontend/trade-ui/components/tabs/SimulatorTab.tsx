'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { submitTrade, getAllTradesFromCache, type TradeDto, type CachedTrade } from '@/lib/api';

export default function SimulatorTab() {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<TradeDto>({
    tradeId: '',
    version: 1,
    counterPartyId: '',
    bookId: '',
    maturityDate: '',
  });

  // Fetch trades from cache
  const { data: trades = [], isLoading, refetch } = useQuery<CachedTrade[]>({
    queryKey: ['trades', 'cache'],
    queryFn: getAllTradesFromCache,
    refetchInterval: 5000, // Refetch every 5 seconds
  });

  // Submit trade mutation
  const submitMutation = useMutation({
    mutationFn: (trade: TradeDto) => submitTrade(trade),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trades', 'cache'] });
      // Reset form
      setFormData({
        tradeId: '',
        version: 1,
        counterPartyId: '',
        bookId: '',
        maturityDate: '',
      });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitMutation.mutate(formData);
  };

  const handleChange = (field: keyof TradeDto, value: string | number) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Left Pane - Trade Form */}
      <Card>
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

            {submitMutation.isSuccess && (
              <div className="p-3 bg-green-50 border border-green-200 rounded text-green-800 text-sm">
                Trade submitted successfully! Status: {submitMutation.data.status}
              </div>
            )}

            {submitMutation.isError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded text-red-800 text-sm">
                Error: {submitMutation.error?.message || 'Failed to submit trade'}
              </div>
            )}
          </form>
        </CardContent>
      </Card>

      {/* Right Pane - Trades List */}
      <Card>
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
            <div className="border rounded-lg overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Trade ID</TableHead>
                    <TableHead>Version</TableHead>
                    <TableHead>Counter Party</TableHead>
                    <TableHead>Book ID</TableHead>
                    <TableHead>Maturity Date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {trades.map((trade, index) => (
                    <TableRow key={index}>
                      <TableCell className="font-medium">
                        {trade.tradeId || trade.TradeId || '-'}
                      </TableCell>
                      <TableCell>{trade.version || trade.Version || '-'}</TableCell>
                      <TableCell>
                        {trade.counterPartyId || trade.CounterPartyId || '-'}
                      </TableCell>
                      <TableCell>{trade.bookId || trade.BookId || '-'}</TableCell>
                      <TableCell>
                        {trade.maturityDate || trade.MaturityDate || '-'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
