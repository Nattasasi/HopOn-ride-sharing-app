"use client";

import * as React from "react";

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  // You can add more props here if needed
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={className} // TailwindCSS classes from parent
        ref={ref}
        {...props}
      />
    );
  }
);
Input.displayName = "Input";

export { Input };
