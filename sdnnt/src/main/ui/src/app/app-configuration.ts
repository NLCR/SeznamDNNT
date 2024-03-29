import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { Configuration } from './shared/configuration';
import { AppState } from './app.state';
import { User } from './shared/user';

@Injectable({
    providedIn: 'root'
}) export class AppConfiguration {

    // version 
    static sdnntClientVersion = 'v1.0.9.7';


    private config: Configuration;
    public invalidServer: boolean;


    public get sortedFacets() {
        return this.config.sortedFacets;
    }

    public get context() {
        return this.config.context;
    }

    public get lang() {
        return this.config.lang;
    }

    public get snackDuration() {
        return this.config.snackDuration;
    }

    public get snackRows() {
        return this.config.rows;
    }

    public get logo() {
        return this.config.logo;
    }

    public get homeTabs() {
        return this.config.homeTabs;
    }

    public get homeCards() {
        return this.config.homeCards;
    }

    public get dntStates() {
        return this.config.dntStates;
    }

    public get identifiers() {
        return this.config.identifiers;
    }

    public get role() {
        return this.config.role;
    }

    public get filterFields() {
        return this.config.filterFields;
    }



    public get userFilterFields() {
        return this.config.userFilterFields;
    }

    public get sorts() {
        return this.config.sorts;
    }


    public get pinginterval() {
        return this.config.pinginterval || 10;
    }

    public get simplelogin() {
        return this.config.simplelogin || false;
    }

    public get maximumItemInRequest() {
        return this.config.maximumItemInRequest || 60;
    }    

    public get dntSetAlpehLinks() {
        return this.config.dntSetAlpehLinks;
    }    

    public get numberOfRuleBasedNotifications() {
        return this.config.numberOfRuleBasedNotifications;
    }    

    public get numberOfItemsInRulenotification() {
        return this.config.numberOfItemsInRulenotification;
    }    

    /**
     * List the files holding section configuration in assets/configs folder
     * ['search'] will look for /assets/configs/search.json
     */
    private configs: string[] = [];

    constructor(
        private http: HttpClient,
        private state: AppState) { }

    public configLoaded() {
        return this.config && true;
    }

    public load(): Promise<any> {
        console.log('loading config...');
        const promise = this.http.get('assets/config.json')
            .toPromise()
            .then(cfg => {
                this.config = cfg as Configuration;
                this.state.setConfig(this.config);
            }).then(() => {
                return this.login();
            });
        return promise;
    }

    private login() {
        if (this.config.user) {
            this.state.setLogged(this.config.user);
            const user: any = JSON.parse(localStorage.getItem('user'));
            if (user) {
                localStorage.setItem('user', JSON.stringify({ username: this.config.user.username, pwd: this.config.user.pwd, timeStamp: Date.now() }));
            }
            return;
        }
        const url = 'api/user/login';
        const user: any = JSON.parse(localStorage.getItem('user'));
        if (user) {
            const now = Date.now();
            const lastLogged = new Date(user.timeStamp).getTime();

            return this.http.post(url, { user: user.username, pwd: user.pwd })
                .toPromise()
                .then((res: any) => {
                    this.state.setLogged(res);
                    if (res.error) {
                        localStorage.removeItem('user');
                    } else {
                        localStorage.setItem('user', JSON.stringify({ username: user.username, pwd: user.pwd, timeStamp: Date.now() }));
                    }
                    
                })
                .catch(res => {
                    console.log(res);
                    localStorage.removeItem('user');
                });
        } else {
            return;
        }
    }

}
