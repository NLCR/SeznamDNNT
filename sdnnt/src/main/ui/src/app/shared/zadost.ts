import { logging } from "selenium-webdriver";

export class Zadost {

  id: string;
  user: string;
  institution: string;
  kurator: string;
  datum_zadani: Date;
  state: string;
  datum_vyrizeni: Date;
  pozadavek: string;
  navrh: string;
  poznamka: string;
  identifiers: string[];
  process: string;

  delegated: string;
  priority: string;
  version: string;

  type_of_deadline: string;
  type_of_period: string;
  deadline: Date;
  desired_item_state: string;
  desired_license: string;
  
  escalated: boolean = false;
  expired: boolean = false;

  email: string;

  constructor(timestamp: string, user: string) {
    this.id = user + timestamp;
    this.user = user;
    this.identifiers = [];
    this.state = 'open';
  }

  // getUserAndInstitution(): string {
  //   if (this.institution != null) return this.user +" ("+this.institution+")";
  //   else return this.user;
  // }
}

