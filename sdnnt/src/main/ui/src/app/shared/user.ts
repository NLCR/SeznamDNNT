import { SolrDocument } from 'src/app/shared/solr-document';
import { UserRegisterOption } from './user-register-option';

export class User {

  
  username: string;
  pwd: string;
  role: string;
  isActive: boolean;
  notifikace_interval:string;

  typ: string; //pravnicka/fyzicka osoba
  titul: string;
  jmeno: string;
  prijmeni: string;
  ico: string;
  adresa: string;
  cislo_popisne: string;
  ulice: string;
  psc: string;
  mesto: string;
  telefon: string;
  email: string;
  kontaktni: string;
  apikey: string;

  institution: string;


  nositel: string[]; // Nositel autorských práv k dílu: 
  poznamka: string;
  error?: string;


  registerOption: UserRegisterOption;
}

