import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppState } from './app.state';
import { Configuration, KrameriusLibrary } from './shared/configuration';

@Injectable({
    providedIn: 'root'
}) export class AppConfiguration {
    private http = inject(HttpClient);
    private state = inject(AppState);


    // version
    static sdnntClientVersion = 'v1.0.9.7';


    private config!: Configuration;
    public invalidServer = false;


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

    public get krameriusLibraries() {
        return this.config.kramerius_libraries || {};
    }

    public get enabledKrameriusLibraries() {
        return this.config.enabled_kramerius_libraries || [];
    }

    public getKrameriusLibrary(value: string): KrameriusLibrary | null {
        if (!value) {
            return null;
        }
        const libraries = this.krameriusLibraries;
        return libraries[value] || libraries[value.toUpperCase()] || libraries[value.toLowerCase()] || null;
    }

    public isEnabledKrameriusLibrary(value: string): boolean {
        if (!value) {
            return false;
        }
        const values = this.enabledKrameriusLibraries;
        return values.includes(value) || values.includes(value.toUpperCase()) || values.includes(value.toLowerCase());
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

    public configLoaded() {
        return this.config && true;
    }

    public load(): Promise<any> {
        console.log('loading config...');
        const promise = firstValueFrom(this.http.get('assets/config.json'))
            .then(cfg => {
                this.config = cfg as Configuration;
                this.state.setConfig(this.config);
            }).then(() => this.login());
        return promise;
    }

    private login() {
        // Remove credentials stored by older releases. Authentication is now cookie-based.
        localStorage.removeItem('user');
        if (this.config.user) {
            this.state.setLogged(this.config.user);
            return;
        }
        return firstValueFrom(this.http.get('api/user/remember_login', { withCredentials: true }))
            .then((res: any) => {
                if (res && res.username) {this.state.setLogged(res);}
            })
            .catch(res => console.log(res));
    }

}
