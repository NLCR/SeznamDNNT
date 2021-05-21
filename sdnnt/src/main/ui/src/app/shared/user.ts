import { SolrDocument } from 'src/app/shared/solr-document';

export class User {
  username: string;
  role: string;

  typ: string; //pravnicka/fyzicka osoba
  titul: string;
  jmeno: string;
  prijmeni: string;
  ico: string;
  adresa: string;
  psc: string;
  mesto: string;
  telefon: string;
  email: string;
  kontaktni: string;

  nositel: string; // Nositel autorských práv k dílu: 
  poznamka: string;
  error?: string;
}

