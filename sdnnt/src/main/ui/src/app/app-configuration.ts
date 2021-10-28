import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { Configuration } from './shared/configuration';
import { AppState } from './app.state';
import { User } from './shared/user';

@Injectable({
    providedIn: 'root'
}) export class AppConfiguration {

    private config: Configuration;
    public invalidServer: boolean;

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

    public get homeTabs() {
        return this.config.homeTabs;
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

    public get sorts() {
        return this.config.sorts;
    }

    public get pinginterval() {
        return this.config.pinginterval || 10;
    }

    //pinginterval: number;

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
            /*
            const isTimeOut = (now - lastLogged)/60000 > this.config.loginTimeOut;
            if (isTimeOut) {
                localStorage.removeItem('user');
                return;
            }*/
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
