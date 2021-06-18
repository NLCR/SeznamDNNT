import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AccountComponent } from './pages/account/account.component';
import { AdminComponent } from './pages/admin/admin.component';
import { HelpComponent } from './pages/help/help.component';
import { HomeComponent } from './pages/home/home.component';
import { ImportComponent } from './pages/import/import.component';
import { ImportsComponent } from './pages/imports/imports.component';
import { SearchComponent } from './pages/search/search.component';
import { ZadostComponent } from './pages/zadost/zadost.component';

const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: 'help', component: HelpComponent },
  { path: 'account', component: AccountComponent },
  { path: 'zadost/:id', component: ZadostComponent },
  { path: 'imports', component: ImportsComponent },
  { path: 'import/:id', component: ImportComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'search', component: SearchComponent},
  { path: '', redirectTo: 'home', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { relativeLinkResolution: 'legacy' })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
