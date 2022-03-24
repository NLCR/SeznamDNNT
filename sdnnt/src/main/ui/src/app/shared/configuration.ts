import { User } from "./user";

export interface Sort { field: string; dir: string};

export interface Configuration {

  context: string;
  lang: string;
  snackDuration: number;
  logo: string;
  homeTabs: string[];
  homeCards: string[];
  simplelogin:boolean;
  maximumItemInRequest: number;
  dntSetAlpehLinks: boolean;
  numberOfRuleBasedNotifications: number;
  numberOfItemsInRulenotification: number;

  // Seznam roli
  role: string[];
  
  // Seznam stavu zaznamu pro role
  dntStates: {[role: string]: string[]};

  // Seznam poli identifikatoru
  identifiers: string[];

  rows: number;

  // Seznam poli, ktere se zpracuju v url jako filter
  filterFields: string[];
  
  // seznam poli 
  userFilterFields: string[],

  // Sort pro standardni data 
  sorts: {[key: string]: Sort[]};
  // // Sort v account
  // sorts_account:  Sort[];

  // User from session
  user: User;

  // ping interval
  pinginterval: number;



}
