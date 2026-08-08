import re, html
x = open(r"C:\Users\Juan\AppData\Local\Temp\opencode\er3.xml", encoding="utf-8").read()
cells = re.findall(r'<mxCell[^>]*vertex="1"[^>]*>', x)
print("vertices:", len(cells))
for v in cells:
    val = re.search(r'value="([^"]*)"', v)
    label = val.group(1) if val else "noval"
    # extract inner text content
    label = re.sub(r'<[^>]+>', "", label)
    print("---", label[:120].replace('\n',' '))
