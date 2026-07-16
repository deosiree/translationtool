#!/usr/bin/env node
/** 输出 workflow_rev 与 workflow_tree_hash（JSON） */
import { workflowRev, workflowTreeHash } from "./lib.mjs";

const out = {
  workflow_rev: workflowRev(),
  workflow_tree_hash: workflowTreeHash(),
};
process.stdout.write(JSON.stringify(out, null, 2) + "\n");
