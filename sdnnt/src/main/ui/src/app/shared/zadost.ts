export class Zadost {
  id: string;
  user: string;
  datum_zadani: Date;
  state: string;
  datum_vyrizeni: Date;
  pozadavek: string;
  new_stav: string;
  poznamka: string;
  identifiers: string[];

  constructor(timestamp: string, user: string) {
    this.id = user + timestamp;
    this.user = user;
    this.identifiers = [];
    this.state = 'open';
  }
}

