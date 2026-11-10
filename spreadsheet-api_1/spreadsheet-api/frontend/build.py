"""Builds the single-file page: inlines design-system tokens, fonts and the EasyRetire logos."""
import base64, pathlib, re, sys
ds = pathlib.Path(sys.argv[1])           # folder holding the downloaded design-system 'project' files
here = pathlib.Path(__file__).parent
tpl = (here / 'src' / 'template.html').read_text()

tokens = (ds / 'tokens.css').read_text()
tokens = tokens.split('.ee-text-xs {')[0]          # keep variables, drop the text classes and font-faces

def font(name, file, weights):
    b64 = base64.b64encode((ds / 'fonts' / file).read_bytes()).decode()
    return ("@font-face{font-family:'%s';src:url(data:font/ttf;base64,%s) format('truetype');"
            "font-weight:%s;font-style:normal;font-display:swap}" % (name, b64, weights))
fonts = font('Stack Sans Headline', 'StackSansHeadline-VariableFont_wght.ttf', '100 900') + '\n' + \
        font('Stack Sans Text', 'StackSansText-VariableFont_wght.ttf', '100 900')

def logo(name):
    return 'data:image/svg+xml;base64,' + base64.b64encode((ds / 'assets' / 'Product logos' / name).read_bytes()).decode()

out = (tpl.replace('/*FONTS*/', fonts).replace('/*TOKENS*/', tokens)
          .replace('__LOGO_LIGHT__', logo('easyretire-logo.svg'))
          .replace('__LOGO_DARK__', logo('easyretire-logo-white.svg')))
(here / 'tax-file-resubmission.html').write_text(out)
print('built', len(out) // 1024, 'KB')
