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

