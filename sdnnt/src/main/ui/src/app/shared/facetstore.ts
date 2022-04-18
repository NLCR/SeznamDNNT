
export class FacetsStore {
    private _facetsUIStore =new Map();

    public initDefault(key: string): void {
        let obj =  {};
        this._facetsUIStore.set(key, obj);
    }

    constructor() {
        this.initDefault('account');   
    }

    reinit() {
        this.initDefault('account');   
    }

    //public setPage(key: string, page:number) {
    setFacet(key: string, facetkey: string, facetval: string ) {
        if (!this._facetsUIStore.has(key)) {
            this.initDefault(key);
        }
        let facets = this._facetsUIStore.get(key);
        facets[facetkey] = facetval;
    }


    checkTheSameAndSet(key: string, facetkey: string, facetval: string ) {
        if (!this._facetsUIStore.has(key)) {
            this.initDefault(key);
        }
        let facets = this._facetsUIStore.get(key);
        if (facets[facetkey] === facetval) {
            delete facets[facetkey];
        } else {
            facets[facetkey] = facetval;
        }
    }


    getFacet(key: string, facetkey: string): string {
        if (this._facetsUIStore.has(key)) {
            let facets = this._facetsUIStore.get(key);
            return facets[facetkey];
        } else return null;
    }


    clearFacets(key: string) {
        if (this._facetsUIStore.has(key)) { 
            this._facetsUIStore.set(key, {});
        }
    }

    clearFacetKey(key: string, facetkey: string) {
        if (!this._facetsUIStore.has(key)) {
            this.initDefault(key);
        }
        let facets = this._facetsUIStore.get(key);
        delete facets[facetkey];
    }


    isFacetSelected(key: string, facetkey: string, facetval: string) {
        if (this._facetsUIStore.has(key)) {
            let facets = this._facetsUIStore.get(key);
            if (facets[facetkey]) {
                return facets[facetkey] === facetval;
            }
        }
        return false;
    }


    


}