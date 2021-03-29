import { User } from "./user";

export interface Sort { label: string; field: string; dir: string};

export interface Configuration {
  context: string;
  lang: string;
  snackDuration: number;
  homeTabs: string[];
  
  // Seznam stavu zaznamu pro role
  dntStates: {[role: string]: string[]};
}
