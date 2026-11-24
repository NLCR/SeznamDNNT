const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = tseslint.config(
  {
    ignores: ['projects/**/*']
  },
  {
    files: ['**/*.ts'],
    extends: [tseslint.configs.base, ...angular.configs.tsRecommended],
    processor: angular.createProcessor(),
    rules: {
      '@typescript-eslint/naming-convention': [
        'error',
        { selector: 'default', format: ['camelCase'] },
        { selector: 'typeLike', format: ['PascalCase'] },
        { selector: 'typeParameter', format: ['PascalCase'] },
        { selector: 'variableLike', format: ['camelCase', 'PascalCase', 'UPPER_CASE', 'snake_case'] },
        { selector: 'property', format: ['camelCase', 'PascalCase', 'UPPER_CASE', 'snake_case'], leadingUnderscore: 'allow' },
        { selector: 'method', format: ['camelCase'], leadingUnderscore: 'allow' },
        { selector: 'enumMember', format: ['camelCase', 'PascalCase', 'UPPER_CASE'] }
      ],
      'no-underscore-dangle': ['error', {
        allow: ['_paramsProcessed', '_stateSubject', '_configSubject', '_facetsUIStore', '_navigationUIStore']
      }],
      'max-len': ['error', { code: 200 }],
      eqeqeq: ['error', 'always', { null: 'ignore' }]
    }
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended],
    rules: {
      '@angular-eslint/template/eqeqeq': ['error', { allowNullOrUndefined: true }]
    }
  }
);
