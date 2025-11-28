'use client';

import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import SimulatorTab from '@/components/tabs/SimulatorTab';
import AuditTab from '@/components/tabs/AuditTab';
import ChatbotTab from '@/components/tabs/ChatbotTab';

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto p-6">
        <h1 className="text-3xl font-bold mb-6">Trade Platform</h1>
        <Tabs defaultValue="simulator" className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="simulator">Simulator</TabsTrigger>
            <TabsTrigger value="audit">Audit</TabsTrigger>
            <TabsTrigger value="chatbot">Chatbot</TabsTrigger>
          </TabsList>
          <TabsContent value="simulator" className="mt-6">
            <SimulatorTab />
          </TabsContent>
          <TabsContent value="audit" className="mt-6">
            <AuditTab />
          </TabsContent>
          <TabsContent value="chatbot" className="mt-6">
            <ChatbotTab />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
