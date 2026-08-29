/**
 * Newman Runner Configuration
 * 
 * Automated Postman Collection testing using Newman CLI.
 * 
 * Usage:
 *   node newman-runner.js                          # Run all tests
 *   node newman-runner.js --env staging             # Run with staging environment
 *   node newman-runner.js --report html             # Generate HTML report
 *   node newman-runner.js --skip-auth               # Skip auth module tests
 * 
 * Environment files:
 *   - OMS_API_Environment.json (default)
 *   - env/omsdb-dev.properties (dev config)
 *   - env/omsdb-staging.properties (staging config)
 *   - env/omsdb-prod.properties (prod config)
 */

const newman = require('newman');
const path = require('path');

// Configuration
const COLLECTION_FILE = path.join(__dirname, 'OMS_API_Collection.json');
const ENVIRONMENT_FILES = {
    dev: 'OMS_API_Environment.json',
    staging: 'OMS_API_Environment-Staging.json',
    prod: 'OMS_API_Environment-Production.json',
};

// Parse command line arguments
const args = process.argv.slice(2);
const options = {
    env: 'dev',
    report: 'cli',
    skipAuth: false,
    skipProduct: false,
    skipOrder: false,
    skipAdmin: false,
    skipSystem: false,
    delayRequest: 100,
    timeout: 10000,
    iterationCount: 1,
    bail: false,
};

// Parse arguments
args.forEach(arg => {
    if (arg.startsWith('--env=')) {
        options.env = arg.split('=')[1];
    } else if (arg.startsWith('--report=')) {
        options.report = arg.split('=')[1];
    } else if (arg === '--skip-auth') {
        options.skipAuth = true;
    } else if (arg === '--skip-product') {
        options.skipProduct = true;
    } else if (arg === '--skip-order') {
        options.skipOrder = true;
    } else if (arg === '--skip-admin') {
        options.skipAdmin = true;
    } else if (arg === '--skip-system') {
        options.skipSystem = true;
    } else if (arg.startsWith('--delay=')) {
        options.delayRequest = parseInt(arg.split('=')[1], 10);
    } else if (arg.startsWith('--timeout=')) {
        options.timeout = parseInt(arg.split('=')[1], 10);
    } else if (arg.startsWith('--iterations=')) {
        options.iterationCount = parseInt(arg.split('=')[1], 10);
    } else if (arg === '--bail') {
        options.bail = true;
    } else if (arg === '--help' || arg === '-h') {
        console.log(`
Newman Runner for OMS API Collection

Usage: node newman-runner.js [options]

Options:
  --env=<name>          Environment to use (dev, staging, prod) [default: dev]
  --report=<type>       Report type (cli, html, json, junit) [default: cli]
  --skip-auth           Skip Auth module tests
  --skip-product        Skip Product module tests
  --skip-order          Skip Order module tests
  --skip-admin          Skip Admin module tests
  --skip-system         Skip System module tests
  --delay=<ms>          Delay between requests in ms [default: 100]
  --timeout=<ms>        Request timeout in ms [default: 10000]
  --iterations=<n>      Number of test iterations [default: 1]
  --bail                Stop on first test failure
  --help, -h            Show this help message

Examples:
  node newman-runner.js
  node newman-runner.js --env=staging --report=html
  node newman-runner.js --skip-auth --bail
  node newman-runner.js --env=prod --timeout=30000 --report=junit
`);
        process.exit(0);
    }
});

// Build folder filter
const folderFilter = [];
if (options.skipAuth) folderFilter.push('Auth');
if (options.skipProduct) folderFilter.push('Product');
if (options.skipOrder) folderFilter.push('Order');
if (options.skipAdmin) folderFilter.push('Admin');
if (options.skipSystem) folderFilter.push('System');

// Reporters configuration
const reporters = ['cli'];
const reporterOptions = {};

if (options.report === 'html' || options.report === 'all') {
    reporters.push('htmlextra');
    reporterOptions.htmlextra = {
        export: `./reports/oms-api-report-${options.env}-${Date.now()}.html`,
        title: `OMS API Test Report - ${options.env.toUpperCase()}`,
        browserTitle: 'OMS API Tests',
        showOnlyFails: false,
        noSyntaxHighlighting: false,
        showEnvironmentData: true,
        skipSensitiveData: true,
        responseTime: true,
    };
}

if (options.report === 'json' || options.report === 'all') {
    reporters.push('json');
    reporterOptions.json = {
        export: `./reports/oms-api-report-${options.env}-${Date.now()}.json`,
    };
}

if (options.report === 'junit' || options.report === 'all') {
    reporters.push('junit');
    reporterOptions.junit = {
        export: `./reports/oms-api-report-${options.env}-${Date.now()}.xml`,
    };
}

// Newman run configuration
const newmanOptions = {
    collection: COLLECTION_FILE,
    environment: path.join(__dirname, ENVIRONMENT_FILES[options.env] || ENVIRONMENT_FILES.dev),
    reporters: reporters,
    reporter: reporterOptions,
    delayRequest: options.delayRequest,
    timeoutRequest: options.timeout,
    iterationCount: options.iterationCount,
    bail: options.bail,
    folder: folderFilter.length > 0 ? folderFilter : undefined,
    color: 'auto',
    insecure: false,
    suppressExitCode: false,
    workingDir: __dirname,
};

console.log('========================================');
console.log('  OMS API - Newman Test Runner');
console.log('========================================');
console.log('');
console.log(`Environment: ${options.env}`);
console.log(`Report Type: ${options.report}`);
console.log(`Delay: ${options.delayRequest}ms`);
console.log(`Timeout: ${options.timeout}ms`);
console.log(`Iterations: ${options.iterationCount}`);
console.log(`Bail on Fail: ${options.bail}`);
console.log('');

// Run Newman
newman.run(newmanOptions, function (err, summary) {
    if (err) {
        console.error('Newman run failed:', err);
        process.exit(1);
    }

    console.log('');
    console.log('========================================');
    console.log('  Test Summary');
    console.log('========================================');
    console.log('');
    console.log(`Collection: ${summary.collection.name}`);
    console.log(`Environment: ${summary.environment.name}`);
    console.log(`Total Requests: ${summary.run.stats.requests.total}`);
    console.log(`Total Assertions: ${summary.run.stats.assertions.total}`);
    console.log(`Passed: ${summary.run.stats.assertions.passed}`);
    console.log(`Failed: ${summary.run.stats.assertions.failed}`);
    console.log(`Pending: ${summary.run.stats.assertions.pending}`);
    console.log(`Total Time: ${summary.run.timings.responseAverage.toFixed(0)}ms avg`);
    console.log('');

    if (summary.run.failures.length > 0) {
        console.log('FAILURES:');
        summary.run.failures.forEach(failure => {
            console.log(`  - ${failure.source.name}: ${failure.error.test}`);
        });
        console.log('');
        process.exit(1);
    } else {
        console.log('✅ All tests passed!');
        console.log('');
        process.exit(0);
    }
});