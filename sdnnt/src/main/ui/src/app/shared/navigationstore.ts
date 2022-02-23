export class NavigationStore {

    private _navigationUIStore =new Map();

    constructor() {
        this.initDefault('imports');
        this.initDefault('account');        
    }

    public contains(key:string) : boolean {
        return this._navigationUIStore.has(key);
    }

    public setRows(key:string, rows:number) : void {
        if (!this._navigationUIStore.has(key)) {
            this.initDefault(key);
        }
        this._navigationUIStore.get(key).rows = rows;
    }

    public getRows(key: string): number {
        if (this._navigationUIStore.has(key)) {
            return this._navigationUIStore.get(key).rows;
        }
    }

    public setPage(key: string, page:number) {
        if (!this._navigationUIStore.has(key)) {
            this.initDefault(key);
        }
        this._navigationUIStore.get(key).page = page;
    }

    public getPage(key: string): number {
        if (this._navigationUIStore.has(key)) {
            return this._navigationUIStore.get(key).page;
        }

    }

    public initDefault(key: string): void {
        let obj =  {'page':0,'rows':20};
        this._navigationUIStore.set(key, obj);
    }

    public findKey(url: string) {
        let found = Array.from(this._navigationUIStore.keys()).find(fragment => url.indexOf(fragment) > 0);
        return found;
    }
}
