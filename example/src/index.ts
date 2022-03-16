import * as gzip from 'gzip-js'
import * as fs from 'fs'
const hello = gzip.zip('hello')
const hello2 = fs.readFileSync('hello.txt')
const hello3 = fs.constants.O_CREAT
