
import { instantiate } from './ktor-sample-web.uninstantiated.mjs';


const exports = (await instantiate({
})).exports;

export const {
memory,
_initialize
} = exports


