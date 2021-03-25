import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { Configuration } from './shared/configuration';
import { AppState } from './app.state';

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
    
    public get homeTabs() {
        return this.config.homeTabs;
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
            });
        return promise;
    }

}
