import { User } from 'src/app/shared/user';

export class UserValidorsResult {
    errorTitle: string;
    errorMessag: string;
    focus: string;

    constructor(eTitle, eMessage, f) {
        this.errorTitle = eTitle;
        this.errorMessag = eMessage;
        this.focus = f;
    }


}

export class UserValidators {

    

    validateFirstNameAndSurname(user:User): UserValidorsResult {
        if (!user.jmeno || user.jmeno.trim() === '')  {
          if (user.typ === 'pravnicka_osoba') {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_kontaktni_osoba_jmeno','kontaktniOsobaJmeno');
        } else {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_jmeno','jmeno');
          }
        }
    
        if (!user.prijmeni || user.prijmeni.trim() === '')  {
          if (user.typ === 'pravnicka_osoba') {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_kontaktni_osoba_prijmeni','kontaktniOsobaPrijmeni');
          } else {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_prijmeni','prijmeni');
          }
        }
      }
    
      validateUserName(user:User): UserValidorsResult {
        const validUsername = user.username && user.username.trim() === user.username.trim().replace(/[^\S]/gi, '');
        if (!user.username || user.username.trim() === '' ||!validUsername ) {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_username','username');
        }
      }
    
    
      validateCompanyName(user:User): UserValidorsResult {
        if (user.typ && user.typ === 'pravnicka_osoba' &&  !user.nazevspolecnosti) {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_nazevspolecnosti','nazevspolecnosti');
        }
      }
    
    
      validateEmail(user:User): UserValidorsResult {
        const validEmail = (user.email && user.email != null && user.email.length > 3 ) ? user.email.trim().match(/^\S+@\S+\.\S+$/) : false;
        if ((!user.email  ||  (user.email && (!user.email.trim().includes('@')) || !validEmail))) {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_email','email');
        }
      }
    
      validICO(user:User): UserValidorsResult {
        const validICO = (user.ico && user.ico != null) ? user.ico.trim().match(/^\d{2}\s*\d{2}\s*\d{2}\s*\d{2}$/) : true;
        if (user.ico  && !validICO) {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_ico','ico');
        } 
      }
    
      validPhoneNumber(user:User): UserValidorsResult {
        const phoneNumberValid = (user.telefon &&  user.telefon != null) ? user.telefon.match(/^\+?(\d{3,4})?(\s*)(\d{3}\s*\d{3}\s*\d{3})$/) : true;
        if (user.telefon && !phoneNumberValid) {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_phonenumber','phonenumber');
        }
      }
    
      validPSCNumber(user:User): UserValidorsResult {
        const pscValid = (user.psc &&  user.psc != null) ? user.psc.match(/^\+?(\d{3})?(\s*)(\d{2})$/) : true;
        if (user.psc && !pscValid) {
            return new UserValidorsResult('alert.registrace_uzivatele_error', 'alert.invalid_psc','psc');
        }
      }
    
    
      basicFieldsValidation(user:User): UserValidorsResult {
        let result: UserValidorsResult;
        result = this.validateUserName(user);
        if (result) { return result; }

        result = this.validateCompanyName(user);
        if (result) { return result; }

        result = this.validateEmail(user);
        if (result) { return result; }

        result = this.validateFirstNameAndSurname(user);
        if (result) { return result; }

        result = this.validICO(user);
        if (result) { return result; }
    
        result = this.validPhoneNumber(user);
        if (result) { return result; }

        result = this.validPSCNumber(user);
        if (result) { return result; }
      }
    

}
  
  