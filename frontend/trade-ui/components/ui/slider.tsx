"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

export interface SliderProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  value?: number;
  onValueChange?: (value: number) => void;
}

const Slider = React.forwardRef<HTMLInputElement, SliderProps>(
  ({ className, value, onValueChange, ...props }, ref) => {
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = parseFloat(e.target.value);
      onValueChange?.(newValue);
    };

    return (
      <input
        type="range"
        ref={ref}
        value={value}
        onChange={handleChange}
        className={cn(
          "w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700",
          "accent-primary",
          className
        )}
        {...props}
      />
    );
  }
);
Slider.displayName = "Slider";

export { Slider };

