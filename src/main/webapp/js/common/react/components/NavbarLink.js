import {setGlobalsIfTesting} from '../../../setTestingGlobals.js';
import {navbarLinkType} from '../navbar_prop_types.js';
setGlobalsIfTesting();

/**
 * Returns a React navbar link component.
 * @param {{url: NavbarLink}} props
 * @return {React.Component} Returns the navbar link.
 */
export function NavbarLink(props) {
  const url = props.url;

  let classes = 'nav-item';
  if (url.href === window.location.pathname) {
    classes += ' active';
  }

  return (
    <div className={classes}>
      <a className="nav-link" href={url.href}>{url.name}</a>
    </div>
  );
}

NavbarLink.propTypes = {
  url: navbarLinkType.isRequired,
};
