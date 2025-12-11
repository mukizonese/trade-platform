"use client";

import React, { useState, useRef, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Slider } from '@/components/ui/slider';
import { sendChatMessage, ChatSource } from '@/lib/aiApi';
import { MessageCircle, ChevronDown, Send, Bot } from 'lucide-react';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  sources?: ChatSource[];
}

export default function AIAssistantWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [minSimilarity, setMinSimilarity] = useState(0.2);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  const handleSend = async () => {
    if (!input.trim() || isLoading) {
      return;
    }

    const userMessage = input.trim();
    setInput('');
    const userMsg: Message = { role: 'user', content: userMessage };
    const newMessages = [...messages, userMsg];
    setMessages(newMessages);
    setIsLoading(true);

    try {
      const response = await sendChatMessage({
        message: userMessage,
        min_similarity: minSimilarity,
        limit: 5,
      });

      const assistantMsg: Message = {
        role: 'assistant',
        content: response.response,
        sources: response.sources
      };
      setMessages([...newMessages, assistantMsg]);
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Failed to get response';
      const errorMessage: Message = {
        role: 'assistant',
        content: 'Error: ' + errorMsg
      };
      setMessages([...newMessages, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSuggestion = (suggestion: string) => {
    setInput(suggestion);
    inputRef.current?.focus();
  };

  const suggestions = [
    "Find similar trades to T5",
    "Explain why this trade T5 is expired",
    "Show me trades with maturity date in 2025",
  ];

  if (!isOpen) {
    return (
      <div className="fixed bottom-6 right-6 z-50">
        <div className="flex flex-col items-center">
          <Button
            onClick={() => setIsOpen(true)}
            className="rounded-full h-14 w-14"
          >
            <Bot className="h-5 w-5" />
          </Button>
          <span className="text-xs mt-1 font-medium">AI</span>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed bottom-6 right-6 z-50 w-96 h-[600px]"> 
      <Card className="flex flex-col h-full">
        <div className="flex items-center justify-between p-4 border-b">
          <div className="flex items-center gap-2">
            <Bot className="h-5 w-5" />
            <h3 className="font-semibold">AI Trade Assistant</h3>
          </div>
          <Button variant="ghost" size="icon-sm" onClick={() => setIsOpen(false)}>
            <ChevronDown className="h-4 w-4" />
          </Button>
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.length === 0 && (
            <div className="text-center py-8">
              <MessageCircle className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p className="text-sm mb-4">Ask something about your trades...</p>
              {suggestions.map((suggestion, idx) => (
                <Button
                  key={idx}
                  variant="outline"
                  size="sm"
                  className="w-full mb-2"
                  onClick={() => handleSuggestion(suggestion)}
                >
                  {suggestion}
                </Button>
              ))}
            </div>
          )}

          {messages.map((msg, idx) => {
            const isUser = msg.role === 'user';
            return (
              <div key={idx} className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
                <div className={`rounded-lg p-3 ${isUser ? 'bg-primary text-white' : 'bg-gray-100'}`}>
                  <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                  {!isUser && msg.sources && msg.sources.length > 0 && (
                    <div className="mt-2 pt-2 border-t">
                      <p className="text-xs mb-1">Sources ({msg.sources.length}):</p>
                      {msg.sources.map((source, i) => {
                        const percentage = source.score ? Math.round(source.score * 100) : 0;
                        return (
                          <div key={i} className="text-xs">
                            Trade {source.tradeId} v{source.version} ; Similarity {source.score} or {percentage}% match
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {isLoading && (
            <div className="flex justify-start">
              <div className="bg-gray-100 rounded-lg p-3">
                <p className="text-sm">Thinking...</p>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="px-4 py-2 border-t">
          <label className="text-xs">Similarity: {minSimilarity.toFixed(1)}</label>
          <Slider
            min={0}
            max={1}
            step={0.1}
            value={minSimilarity}
            onValueChange={setMinSimilarity}
            className="w-full mt-2"
          />
        </div>

        <div className="p-4 border-t">
          <div className="flex gap-2">
            <Input
              ref={inputRef}
              placeholder="Ask something..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleSend();
                }
              }}
              disabled={isLoading}
            />
            <Button onClick={handleSend} disabled={!input.trim() || isLoading}>
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}

