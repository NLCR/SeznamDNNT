export class Zadost {
  id: string;
  user: string;
  kurator: string;
  datum_zadani: Date;
  state: string;
  datum_vyrizeni: Date;
  pozadavek: string;
  navrh: string;
  poznamka: string;
  identifiers: string[];
  process: string;

  constructor(timestamp: string, user: string) {
    this.id = user + timestamp;
    this.user = user;
    this.identifiers = [];
    this.state = 'open';
  }
}

