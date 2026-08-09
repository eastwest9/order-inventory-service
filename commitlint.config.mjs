export default {
    extends: ['@commitlint/config-conventional'],
  
    rules: {
      'type-enum': [
        2,
        'always',
        [
          'feat',
          'fix',
          'refactor',
          'perf',
          'test',
          'docs',
          'style',
          'build',
          'ci',
          'chore',
          'revert',
        ],
      ],
  
      'scope-empty': [2, 'never'],
  
      'scope-enum': [
        2,
        'always',
        [
          'product',
          'inventory',
          'order',
          'member',
          'api',
          'db',
          'backend',
          'frontend',
          'common',
          'load-test',
          'infra',
          'ci',
          'docs',
          'project',
          'repo',
        ],
      ],
  
      'subject-empty': [2, 'never'],
      'subject-case': [0],
      'header-max-length': [2, 'always', 100],
    },
  };