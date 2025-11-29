'use client';

import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { getAuditLogs, type AuditDto } from '@/lib/auditApi';

export default function AuditTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['auditLogs'],
    queryFn: () => getAuditLogs(0, 50),
    refetchInterval: 5000, // Refetch every 5 seconds
  });

  const formatDate = (dateValue: string | null | undefined): string => {
    if (!dateValue) return '-';
    try {
      const date = new Date(dateValue);
      if (isNaN(date.getTime())) return dateValue;
      return date.toLocaleString();
    } catch {
      return dateValue;
    }
  };

  const formatValue = (value: string | number | null | undefined): string => {
    if (value === null || value === undefined) return '-';
    return String(value);
  };

  return (
    <div className="p-6">
      <Card>
        <CardHeader>
          <CardTitle>Audit Logs</CardTitle>
          <CardDescription>
            Latest audit events from trade submissions (refreshes every 5 seconds)
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">Loading audit logs...</div>
          ) : error ? (
            <div className="text-center py-8 text-red-600">
              Error loading audit logs: {error instanceof Error ? error.message : 'Unknown error'}
            </div>
          ) : !data || data.content.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">No audit logs found</div>
          ) : (
            <div className="border rounded-lg overflow-x-auto">
              <Table className="min-w-full">
                <TableHeader>
                  <TableRow>
                    <TableHead className="whitespace-nowrap">Trade ID</TableHead>
                    <TableHead className="whitespace-nowrap">Version</TableHead>
                    <TableHead className="whitespace-nowrap">Book ID</TableHead>
                    <TableHead className="whitespace-nowrap">Counter Party ID</TableHead>
                    <TableHead className="whitespace-nowrap">Maturity Date</TableHead>
                    <TableHead className="whitespace-nowrap">Expired</TableHead>
                    <TableHead className="whitespace-nowrap">Created Date</TableHead>
                    <TableHead className="whitespace-nowrap">Last Updated</TableHead>
                    <TableHead className="whitespace-nowrap">Source</TableHead>
                    <TableHead className="whitespace-nowrap">Validation Status</TableHead>
                    <TableHead className="whitespace-nowrap">Event Type</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((audit: AuditDto) => (
                    <TableRow key={audit.auditId}>
                      <TableCell className="font-medium">
                        {formatValue(audit.tradeId)}
                      </TableCell>
                      <TableCell>{formatValue(audit.version)}</TableCell>
                      <TableCell>{formatValue(audit.bookId)}</TableCell>
                      <TableCell>{formatValue(audit.counterPartyId)}</TableCell>
                      <TableCell>{formatValue(audit.maturityDate)}</TableCell>
                      <TableCell>{formatValue(audit.expired)}</TableCell>
                      <TableCell>{formatDate(audit.createdDate)}</TableCell>
                      <TableCell>{formatDate(audit.lastUpdatedAt)}</TableCell>
                      <TableCell>{formatValue(audit.source)}</TableCell>
                      <TableCell>{formatValue(audit.validationStatus)}</TableCell>
                      <TableCell>{formatValue(audit.eventType)}</TableCell>
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
