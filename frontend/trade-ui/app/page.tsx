'use client';

import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import SimulatorTab from '@/components/tabs/SimulatorTab';
import AuditTab from '@/components/tabs/AuditTab';
import ChatbotTab from '@/components/tabs/ChatbotTab';
import ArchitectureTab from '@/components/tabs/ArchitectureTab';

import Image from "next/image";

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto p-6">

        <Tabs defaultValue="simulator" className="w-full">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="simulator">Simulator</TabsTrigger>
            <TabsTrigger value="audit">Audit</TabsTrigger>
            <TabsTrigger value="chatbot">Chatbot</TabsTrigger>
			<TabsTrigger value="architecture">Architecture</TabsTrigger>
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
		  <TabsContent value="architecture" className="mt-6">
	        <ArchitectureTab />
	      </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
