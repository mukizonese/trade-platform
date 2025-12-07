import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import Image from "next/image";

export default function ArchitectureTab() {
  return (
    <div className="p-6 border rounded-lg">
 
	  <Tabs defaultValue="architecture" className="w-full">
	    <TabsList className="grid w-full grid-cols-4">
	      <TabsTrigger value="architecture">Architecture Diagram</TabsTrigger>
	      <TabsTrigger value="deployment">Deployment Diagram</TabsTrigger>
	      <TabsTrigger value="sequence">Sequence Diagram</TabsTrigger>
	    </TabsList>
	    <TabsContent value="architecture" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/architecture.svg" alt="architecture" width={640} height={600} priority />
		</div>
	    </TabsContent>
	    <TabsContent value="deployment" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/deployment.svg" alt="deployment" width={640} height={600} priority />
		</div>
	    </TabsContent>
	    <TabsContent value="sequence" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/sequence.svg" alt="sequence" width={1540} height={1500} priority />
		</div>
	    </TabsContent>

	  </Tabs>
	  
	  
    </div>
  );
}

