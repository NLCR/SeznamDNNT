import { Component, OnInit } from '@angular/core';
import { AngularEditorConfig } from '@kolkov/angular-editor';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { User } from 'src/app/shared/user';
import { FormControl } from '@angular/forms'; // autocomplete
import { Observable, Subject } from 'rxjs'; // autocomplete
import { map, startWith, debounce, debounceTime } from 'rxjs/operators'; // autocomplete
import { UserValidators } from 'src/app/shared/uservalidators';


@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {


  private subject: Subject<string> = new Subject();

  public htmlContent: string;
  public selected: string = 'news';

  editorConfig: AngularEditorConfig = {
    editable: true,
    spellcheck: true,
    height: 'auto',
    minHeight: '0',
    maxHeight: 'auto',
    width: 'auto',
    minWidth: '0',
    translate: 'yes',
    enableToolbar: true,
    showToolbar: true,
    placeholder: 'Enter text here...',
    defaultParagraphSeparator: '',
    defaultFontName: '',
    defaultFontSize: '',
    fonts: [
      { class: 'arial', name: 'Arial' },
      { class: 'times-new-roman', name: 'Times New Roman' },
      { class: 'calibri', name: 'Calibri' },
      { class: 'comic-sans-ms', name: 'Comic Sans MS' }
    ],
    customClasses: [
      {
        name: 'quote',
        class: 'quote',
      },
      {
        name: 'redText',
        class: 'redText'
      },
      {
        name: 'titleText',
        class: 'titleText',
        tag: 'h1',
      },
    ],
    uploadUrl: 'v1/image',
    uploadWithCredentials: false,
    sanitize: true,
    toolbarPosition: 'top',
    toolbarHiddenButtons: [
      /* ['bold', 'italic'],
      ['fontSize'] */
    ]
  };

  public users: User[];
  selUser: User;
  focus: string;
  //userFilterValue: string ="";

  constructor(
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.state.activePage = 'Admin';
    this.service.getUsersByPrefix("").subscribe(res => {
      this.users = res.docs;
      this.selUser = this.users[0];
    });
    this.service.langChanged.subscribe(l => { 
      this.getText(); 
    });
    this.getText();

    this.subject.pipe(
      debounceTime(400)
    ).subscribe(searchTextValue => {
      this.filterUsers(searchTextValue);
    });

  }

  getText() {

    this.service.getText(this.selected).subscribe(text => this.htmlContent = text);
  }

  editorTabs(): string[] {
    let helpFiles: string[] = ['help', 'help_user', 'help_admin', 'help_knihovna', 'help_kurator', 'help_mainKurator'];
    return this.config.homeTabs.concat(helpFiles);
    //return this.config.homeTabs;
  }

  selectText(id: string) {
    this.selected = id;
    this.getText();
  }

  selectUser(user: User) {
    this.selUser = user;
  }

  onFilterUsersKeyUp(target) {
    this.subject.next(target.value);
  }

  filterUsers(fval) {
    this.service.getUsersByPrefix(fval).subscribe(res => {
      if (res.docs.length > 0) {
        this.users = res.docs;
        this.selUser = this.users[0];
      }
    });
  }

  saveText() {
    this.service.saveText(this.selected, this.htmlContent).subscribe();
  }

  saveUser() {

    let basicFieldsValidators: UserValidators = new UserValidators();
    let result = basicFieldsValidators.basicFieldsValidation(this.selUser);
    if (result) {
      this.service.showSnackBar(result.errorTitle, result.errorMessag, true);
      this.focus = result.focus;
      return;
    }


    this.service.saveUser(this.selUser).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_uzivatele_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_uzivatele_success', '', false);
      }
    });
  }


  resetPwd() {
    this.service.adminResetPwd(this.selUser.username).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.reset_hesla_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.reset_hesla_success', '', false);
      }
    });
  }

}
