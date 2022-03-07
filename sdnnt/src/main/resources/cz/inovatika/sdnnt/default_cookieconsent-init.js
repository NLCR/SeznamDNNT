var DOMAIN = "{{domain}}";
var DEFAULT_LANGUAGE = "{{language}}";

// obtain cookieconsent plugin
var cc = initCookieConsent();

cc.run({
    current_lang: DEFAULT_LANGUAGE,
    autoclear_cookies: true,                    // default: false
    cookie_name: 'cc_cookie_sdnnt',             // default: 'cc_cookie'
    cookie_expiration: 365,                     // default: 182
    page_scripts: true,                         // default: false
    force_consent: true,                        // default: false


    gui_options: {
        consent_modal: {
            layout: 'cloud',                    // box,cloud,bar
            position: 'bottom center',          // bottom,middle,top + left,right,center
            transition: 'slide'                 // zoom,slide
        },
        settings_modal: {
            layout: 'bar',                      // box,bar
            position: 'left',                   // right,left (available only if bar layout selected)
            transition: 'slide'                 // zoom,slide
        }
    },

    onFirstAction: function(){
	},

    onAccept: function (cookie) {},

    onChange: function (cookie, changed_preferences) {
        // If analytics category is disabled => disable google analytics
        if (!cc.allowedCategory('analytics')) {
            typeof gtag === 'function' && gtag('consent', 'update', {
                'analytics_storage': 'denied'
            });
        }
    },

    languages: {
        'cs': {
            consent_modal: {
                title: 'Souhlas s používáním cookies',
                description: 'Soubory cookies používáme ke zlepšení našich webových stránek, možnosti personifikace stránek a hlavně abychom vám poskytli komfort při používání těchto stránek',
                primary_btn: {
                    text: 'Přijmout vše',
                    role: 'accept_all'      //'accept_selected' or 'accept_all'
                },
                secondary_btn: {
                    text: 'Nastavení',
                    role: 'settings'       //'settings' or 'accept_necessary'
                },
                revision_message: '<br><br> Dear user, terms and conditions have changed since the last time you visisted!'
            },
            settings_modal: {
                title: 'Nastavení',
                save_settings_btn: 'Uložit nastavení',
                accept_all_btn: 'Přijmout vše',
                reject_all_btn: 'Odmítnout vše',
                close_btn_label: 'Zavrít',
                cookie_table_headers: [
                    {col1: 'Jméno'},
                    {col2: 'Doména'},
                    {col3: 'Popis'},
                    {col4: 'Expirace'},
                    {col5: 'Zpracovatel'},
                ],
                blocks: [
                    {
                        title: 'Použití cookies',
                        description: 'Soubory cookies používáme ke zlepšení našich webových stránek, možnosti personifikace stránek a hlavně abychom vám poskytli komfort při používání těchto stránek.'
                    }, {
                        title: 'Nezbytné cookies',
                        description: 'Nezbytné cookies jsou důležité pro zobrazení této webové stránky. Ukládá se zde i souhlas ohledně nastavení Cookie.',
                        toggle: {
                            value: 'necessary',
                            enabled: true,
                            readonly: true  //cookie categories with readonly=true are all treated as "necessary cookies"
                        },
						cookie_table: [
                            {
                                col1: 'cc_cookie_sdnnt',
                                col2: DOMAIN,
                                col3: 'Ukládá souhlas uživatele s používáním Cookies',
                                col4: 'platnost: 1 rok',
                                col5: 'Národní knihovna České republiky'
                            },
                            {
                                col1: 'JSESSION_ID',
                                col2: DOMAIN,
                                col3: 'Drží informace o přihlášeném uživateli',
                                col4: 'platnost: Při neaktivitě uživatele 10 minut',
                                col5: 'Národní knihovna České republiky'
                            }
                        ]

                    }
					, 
					{
                        title: 'Analytické cookies',
                        description: 'Analytické cookies nám pomámhají vylepšit naše webové stránky shromažďováním a hlášením informací o tom, jak je používáte. Tento způsob přímo nikoho neidentifikuje.',
                        toggle: {
                            value: 'analytics',
                            enabled: false,
                            readonly: false
                        },
                        cookie_table: [
                            {
                                col1: '^_ga',
                                col2: DOMAIN,
                                col3: 'Ukládá unikátní ID, které slouží pro generování statistických dat s informacemi o využívání webu ze strany uživatele',
                                col4: '2 roky',
                                col5: 'Google Inc'
                            },
                            {
                                col1: '_gid',
                                col2: DOMAIN,
                                col3: 'Ukládá unikátní ID, které slouží pro generování statistických dat s informacemi o využívání webu ze strany uživatele',
                                col4: '1 den',
                                col5: 'Google Inc'
                            },
                            {
                                col1: '_gat',
                                col2: DOMAIN,
                                col3: 'Reguluje provoz a předchází problémům v Google Analytics',
                                col4: '1 den',
                                col5: 'Google Inc'
                            }
							
                        ]
                    }
                ]
            }
        },
		
        'en': {
            consent_modal: {
                title: 'Our use of cookies',
                description: "We use the necessary cookies to make our site work. We'd also like to set analytics cookies that help us make improvements by measuring how you use the site. These will be set only if you accept.",
                primary_btn: {
                    text: 'Accept all',
                    role: 'accept_all'      //'accept_selected' or 'accept_all'
                },
                secondary_btn: {
                    text: 'Settings',
                    role: 'settings'       //'settings' or 'accept_necessary'
                }
            },
            settings_modal: {
                title: 'Settings',
                save_settings_btn: 'Save current settings',
                accept_all_btn: 'Accept all',
                reject_all_btn: 'Reject all',
                close_btn_label: 'Close',
                cookie_table_headers: [
                    {col1: 'Name'},
                    {col2: 'Domain'},
                    {col3: 'Description'},
                    {col4: 'Expiration'},
                    {col5: 'Controller'},
                ],
                blocks: [
                    {
                        title: 'Cookie usage',
                        description: "We use the necessary cookies to make our site work. We'd also like to set analytics cookies that help us make improvements by measuring how you use the site. These will be set only if you accept."
                    }, {
                        title: 'Nezbytné cookies',
                        description: 'Necessary cookies enable core functionality such as security, network management, and accessibility. You may disable these by changing your browser settings, but this may affect how the website functions.',
                        toggle: {
                            value: 'necessary',
                            enabled: true,
                            readonly: true  //cookie categories with readonly=true are all treated as "necessary cookies"
                        },
						cookie_table: [
                            {
                                col1: 'cc_cookie_sdnnt',
                                col2: DOMAIN,
                                col3: "Stores the user's consent to the use of cookies",
                                col4: '1 year',
                                col5: 'Národní knihovna České republiky'
                            },
                            {
                                col1: 'JSESSION_ID',
                                col2: DOMAIN,
                                col3: "Kees user's  session ",
                                col4: '10 minutes in case of user inactivity',
                                col5: 'Národní knihovna České republiky'
                            }
                        ]

                    }
					, 
					{
                        title: 'Analytics Cookies',
                        description: "We'd like to set Google Analytics cookies to help us improve our website by collecting and reporting information on how you use it. The cookies collect information in a way that does not directly identify anyone.",
                        toggle: {
                            value: 'analytics',
                            enabled: false,
                            readonly: false
                        },
                        cookie_table: [
                            {
                                col1: '^_ga',
                                col2: DOMAIN,
                                col3: "Registers a unique ID that is used to generate statistical data on how the visitor uses the web site",
                                col4: '2 years',
                                col5: 'Google Inc'
                            },
                            {
                                col1: '_gid',
                                col2: DOMAIN,
                                col3: "Registers a unique ID that is used to generate statistical data on how the visitor uses the web site",
                                col4: '1 day',
                                col5: 'Google Inc'
                            },
                            {
                                col1: '_gat',
                                col2: DOMAIN,
                                col3: 'Used by Google Analytics to throttle request rate',
                                col4: '1 day',
                                col5: 'Google Inc'
                            }
							
                        ]
                    }
                ]
            }
        },
		
		
    }
});