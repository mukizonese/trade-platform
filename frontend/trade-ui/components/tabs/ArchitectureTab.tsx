import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import Image from "next/image";

export default function ArchitectureTab() {
  return (
    <div className="p-4 border rounded-lg">
 
	  <Tabs defaultValue="architecture" className="w-full">
	    <TabsList className="grid w-full grid-cols-4">
	      <TabsTrigger value="architecture">Architecture Diagram</TabsTrigger>
	      <TabsTrigger value="deployment">Deployment Diagram</TabsTrigger>
	      <TabsTrigger value="sequence">Sequence Diagram Trade</TabsTrigger>
		  <TabsTrigger value="sequence-ai">Sequence Diagram AI</TabsTrigger>
	    </TabsList>
	    <TabsContent value="architecture" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/architecture.svg" alt="architecture" width={940} height={800} priority />
		</div>
	    </TabsContent>
	    <TabsContent value="deployment" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/deployment.svg" alt="deployment" width={940} height={900} priority />
		</div>
	    </TabsContent>
	    <TabsContent value="sequence" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/sequence.svg" alt="sequence" width={1540} height={1500} priority />
		</div>
	    </TabsContent>
		<TabsContent value="sequence-ai" className="mt-6">
		<div className="flex gap-3 items-center justify-center"> 	
		    <Image className="dark:invert" src="/sequence-ai.svg" alt="sequence-ai" width={1540} height={1500} priority />
		</div>
		</TabsContent>
	  </Tabs>
	  
	  
    </div>
  );
}

