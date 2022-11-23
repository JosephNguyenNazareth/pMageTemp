from adapter import Adapter
import sys

new_adapter = Adapter()
new_adapter.load_repo(sys.argv[1])
if len(sys.argv) == 3:
    if sys.argv[2] == "all":
        new_adapter.get_latest_commit(True)
else:
    new_adapter.get_latest_commit(False)